package org.graalvm.compiler.truffle.compiler.substitutions;

import org.graalvm.compiler.core.common.calc.CanonicalCondition;
import org.graalvm.compiler.core.common.memory.MemoryOrderMode;
import org.graalvm.compiler.nodes.ConditionAnchorNode;
import org.graalvm.compiler.nodes.ConstantNode;
import org.graalvm.compiler.nodes.LogicNode;
import org.graalvm.compiler.nodes.NamedLocationIdentity;
import org.graalvm.compiler.nodes.NodeView;
import org.graalvm.compiler.nodes.ValueNode;
import org.graalvm.compiler.nodes.calc.AddNode;
import org.graalvm.compiler.nodes.calc.CompareNode;
import org.graalvm.compiler.nodes.calc.MulNode;
import org.graalvm.compiler.nodes.extended.GuardedUnsafeLoadNode;
import org.graalvm.compiler.nodes.extended.RawLoadNode;
import org.graalvm.compiler.nodes.extended.RawStoreNode;
import org.graalvm.compiler.nodes.graphbuilderconf.GraphBuilderContext;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin.Receiver;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugin.RequiredInvocationPlugin;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins;
import org.graalvm.compiler.nodes.graphbuilderconf.InvocationPlugins.Registration;
import org.graalvm.compiler.nodes.java.LoadFieldNode;
import org.graalvm.word.LocationIdentity;

import jdk.vm.ci.meta.JavaKind;
import jdk.vm.ci.meta.ResolvedJavaMethod;

public class TruffleUnsafeHostGraphBuilderPlugins {

    private static final LocationIdentity TAGS_LOCATION_IDENTITY = NamedLocationIdentity.mutable("FrameWithoutBoxing: indexedTags");
    private static final LocationIdentity PRIMITIVE_LOCATION_IDENTITY = NamedLocationIdentity.mutable("FrameWithoutBoxing: indexedPrimitiveLocals");
    private static final LocationIdentity OBJECT_LOCATION_IDENTITY = NamedLocationIdentity.mutable("FrameWithoutBoxing: indexedLocals");

    public static void registerInvocationPlugins(InvocationPlugins plugins) {
        registerFramePlugins(plugins);
    }

    private static void registerFramePlugins(InvocationPlugins plugins) {
        plugins.registerIntrinsificationPredicate(t -> t.getName().equals("Lcom/oracle/truffle/api/impl/FrameWithoutBoxing;"));
        Registration tl = new InvocationPlugins.Registration(plugins, "com.oracle.truffle.api.impl.FrameWithoutBoxing");

        registerUnsafeLoadStorePrimitives(tl, JavaKind.Long, PRIMITIVE_LOCATION_IDENTITY);
        registerUnsafeLoadStorePrimitives(tl, JavaKind.Object, OBJECT_LOCATION_IDENTITY);
        registerUnsafeTagPrimitives(tl);
    }

    private static void registerUnsafeLoadStorePrimitives(Registration tl, JavaKind kind, LocationIdentity locationIdentity) {
        tl.register(new RequiredInvocationPlugin("unsafeGet" + kind.name(), Object.class, long.class, boolean.class, Object.class) {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode object, ValueNode offset, ValueNode condition, ValueNode location) {
                LogicNode compare = b.add(
                                CompareNode.createCompareNode(b.getConstantReflection(), b.getMetaAccess(), b.getOptions(), null, CanonicalCondition.EQ, condition,
                                                ConstantNode.forBoolean(true, object.graph()), NodeView.DEFAULT));
                ConditionAnchorNode anchor = b.add(new ConditionAnchorNode(compare));
                b.addPush(kind, b.add(new GuardedUnsafeLoadNode(object, offset, kind, locationIdentity, anchor, false, MemoryOrderMode.PLAIN)));
                return true;
            }
        });

        tl.register(new RequiredInvocationPlugin("unsafePut" + kind.name(), Object.class, long.class, getJavaClass(kind), Object.class) {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode object, ValueNode offset, ValueNode value, ValueNode location) {
                b.add(new RawStoreNode(object, offset, value, kind, locationIdentity, false, MemoryOrderMode.PLAIN, null, false));
                return true;
            }
        });
    }

    private static void registerUnsafeTagPrimitives(Registration tl) {
        tl.register(new RequiredInvocationPlugin("unsafeVerifyIndexedSet", Receiver.class, int.class, byte.class) {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode slot, ValueNode tag) {
                ValueNode indexedTags = b.add(createLoadIndexedTags(b, receiver));
                int byteArrayBase = b.getMetaAccess().getArrayBaseOffset(JavaKind.Byte);
                int byteArrayIndexScale = b.getMetaAccess().getArrayIndexScale(JavaKind.Byte);
                b.add(new RawStoreNode(indexedTags, createIndex(slot, byteArrayBase, byteArrayIndexScale), tag, JavaKind.Byte, TAGS_LOCATION_IDENTITY, false,
                                MemoryOrderMode.PLAIN));
                return true;
            }
        });
        tl.register(new RequiredInvocationPlugin("unsafeGetIndexedTag", Receiver.class, int.class) {
            @Override
            public boolean apply(GraphBuilderContext b, ResolvedJavaMethod targetMethod, Receiver receiver, ValueNode slot) {
                ValueNode indexedTags = b.add(createLoadIndexedTags(b, receiver));
                int byteArrayBase = b.getMetaAccess().getArrayBaseOffset(JavaKind.Byte);
                int byteArrayIndexScale = b.getMetaAccess().getArrayIndexScale(JavaKind.Byte);

                b.addPush(JavaKind.Byte, new RawLoadNode(indexedTags, createIndex(slot, byteArrayBase, byteArrayIndexScale), JavaKind.Byte, TAGS_LOCATION_IDENTITY, false,
                                MemoryOrderMode.PLAIN));
                return true;
            }
        });

    }

    private static Class<?> getJavaClass(JavaKind kind) {
        return kind == JavaKind.Object ? Object.class : kind.toJavaClass();
    }

    private static ValueNode createLoadIndexedTags(GraphBuilderContext b, Receiver receiver) {
        KnownTruffleTypes types = new KnownTruffleTypes(b.getMetaAccess());

        ValueNode indexedTags = LoadFieldNode.create(
                        b.getConstantFieldProvider(),
                        b.getConstantReflection(),
                        b.getMetaAccess(),
                        b.getOptions(),
                        b.getAssumptions(),
                        receiver.get(),
                        types.fieldIndexedTags,
                        true,
                        true);
        return indexedTags;
    }

    private static ValueNode createIndex(ValueNode index, int base, int scale) {
        ValueNode result = index;
        if (scale != 1) {
            result = new MulNode(result, ConstantNode.forInt(scale));
        }
        if (base != 0) {
            result = new AddNode(result, ConstantNode.forInt(base));
        }
        return result;
    }

}
