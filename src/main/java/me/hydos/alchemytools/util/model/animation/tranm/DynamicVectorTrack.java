// automatically generated by the FlatBuffers compiler, do not modify

package me.hydos.alchemytools.util.model.animation.tranm;

import com.google.flatbuffers.BaseVector;
import com.google.flatbuffers.Constants;
import com.google.flatbuffers.FlatBufferBuilder;
import com.google.flatbuffers.Table;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

@SuppressWarnings("unused")
public final class DynamicVectorTrack extends Table {
  public static void ValidateVersion() { Constants.FLATBUFFERS_23_3_3(); }
  public static DynamicVectorTrack getRootAsDynamicVectorTrack(ByteBuffer _bb) { return getRootAsDynamicVectorTrack(_bb, new DynamicVectorTrack()); }
  public static DynamicVectorTrack getRootAsDynamicVectorTrack(ByteBuffer _bb, DynamicVectorTrack obj) { _bb.order(ByteOrder.LITTLE_ENDIAN); return (obj.__assign(_bb.getInt(_bb.position()) + _bb.position(), _bb)); }
  public void __init(int _i, ByteBuffer _bb) { __reset(_i, _bb); }
  public DynamicVectorTrack __assign(int _i, ByteBuffer _bb) { __init(_i, _bb); return this; }

  public Vec3f vec(int j) { return vec(new Vec3f(), j); }
  public Vec3f vec(Vec3f obj, int j) { int o = __offset(4); return o != 0 ? obj.__assign(__vector(o) + j * 12, bb) : null; }
  public int vecLength() { int o = __offset(4); return o != 0 ? __vector_len(o) : 0; }
  public Vec3f.Vector vecVector() { return vecVector(new Vec3f.Vector()); }
  public Vec3f.Vector vecVector(Vec3f.Vector obj) { int o = __offset(4); return o != 0 ? obj.__assign(__vector(o), 12, bb) : null; }

  public static int createDynamicVectorTrack(FlatBufferBuilder builder,
      int vecOffset) {
    builder.startTable(1);
    DynamicVectorTrack.addVec(builder, vecOffset);
    return DynamicVectorTrack.endDynamicVectorTrack(builder);
  }

  public static void startDynamicVectorTrack(FlatBufferBuilder builder) { builder.startTable(1); }
  public static void addVec(FlatBufferBuilder builder, int vecOffset) { builder.addOffset(0, vecOffset, 0); }
  public static void startVecVector(FlatBufferBuilder builder, int numElems) { builder.startVector(12, numElems, 4); }
  public static int endDynamicVectorTrack(FlatBufferBuilder builder) {
    int o = builder.endTable();
    return o;
  }

  public static final class Vector extends BaseVector {
    public Vector __assign(int _vector, int _element_size, ByteBuffer _bb) { __reset(_vector, _element_size, _bb); return this; }

    public DynamicVectorTrack get(int j) { return get(new DynamicVectorTrack(), j); }
    public DynamicVectorTrack get(DynamicVectorTrack obj, int j) {  return obj.__assign(Table.__indirect(__element(j), bb), bb); }
  }
}

