package me.hydos.alchemytools.util.assimp;

import me.hydos.alchemytools.renderer.scene.ModelData;
import me.hydos.alchemytools.scene.property.ARGBCpuTexture;
import me.hydos.alchemytools.util.ModelLocator;
import me.hydos.alchemytools.util.model.Mesh;
import me.hydos.alchemytools.util.model.SceneNode;
import me.hydos.alchemytools.util.model.animation.BoneNode;
import me.hydos.alchemytools.util.model.animation.Skeleton;
import me.hydos.alchemytools.util.model.bone.Bone;
import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.BufferUtils;
import org.lwjgl.assimp.*;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Objects.requireNonNull;

public class AssimpModelLoader {

    public static SceneNode[] load(String name, ModelLocator locator, int extraFlags) {
        var fileIo = AIFileIO.create()
                .OpenProc((pFileIO, pFileName, openMode) -> {
                    var fileName = MemoryUtil.memUTF8(pFileName);
                    var bytes = locator.getFile(fileName);
                    var data = BufferUtils.createByteBuffer(bytes.length);
                    data.put(bytes);
                    data.flip();

                    return AIFile.create()
                            .ReadProc((pFile, pBuffer, size, count) -> {
                                var max = Math.min(data.remaining() / size, count);
                                MemoryUtil.memCopy(MemoryUtil.memAddress(data), pBuffer, max * size);
                                data.position((int) (data.position() + max * size));
                                return max;
                            })
                            .SeekProc((pFile, offset, origin) -> {
                                switch (origin) {
                                    case Assimp.aiOrigin_CUR -> data.position(data.position() + (int) offset);
                                    case Assimp.aiOrigin_SET -> data.position((int) offset);
                                    case Assimp.aiOrigin_END -> data.position(data.limit() + (int) offset);
                                }

                                return 0;
                            })
                            .FileSizeProc(pFile -> data.limit())
                            .address();
                })
                .CloseProc((pFileIO, pFile) -> {
                    var aiFile = AIFile.create(pFile);
                    aiFile.ReadProc().free();
                    aiFile.SeekProc().free();
                    aiFile.FileSizeProc().free();
                });

        var scene = Assimp.aiImportFileEx(name, Assimp.aiProcess_Triangulate | Assimp.aiProcess_JoinIdenticalVertices | extraFlags, fileIo);
        if (scene == null) throw new RuntimeException(Assimp.aiGetErrorString());
        var rootNode = scene.mRootNode();

        var nodes = new ArrayList<SceneNode>();
        readNodeTree(scene, scene.mRootNode(), null, nodes);

        Assimp.aiReleaseImport(scene);
        return nodes.toArray(SceneNode[]::new);
    }

    private static void readNodeTree(AIScene scene, AINode node, Matrix4f rootTransform, List<SceneNode> nodes) {
        if(rootTransform == null) rootTransform = BoneNode.from(node.mTransformation());

        for (int i = 0; i < node.mNumChildren(); i++) {
            var object = AINode.create(node.mChildren().get(i));
            nodes.add(readScene(scene, object, rootTransform));
            readNodeTree(scene, object, rootTransform, nodes);
        }
    }

    private static SceneNode readScene(AIScene scene, AINode node, Matrix4f parentTransform) {
        var skeleton = new Skeleton(BoneNode.create(node));
        var materials = readMaterialData(scene);
        var textures = readTextureData(scene);
        var meshes = readMeshData(skeleton, scene, node, new HashMap<>());
        var transform = BoneNode.from(node.mTransformation()).mul(parentTransform);
        return new SceneNode(node.mName().dataString(), transform, materials, textures, meshes, skeleton);
    }

    private static Mesh[] readMeshData(Skeleton skeleton, AIScene scene, AINode node, Map<String, Bone> boneMap) {
        var meshes = new Mesh[node.mNumMeshes()];

        for (var i = 0; i < node.mNumMeshes(); i++) {
            var mesh = AIMesh.create(scene.mMeshes().get(node.mMeshes().get(i)));
            var name = mesh.mName().dataString();
            var material = mesh.mMaterialIndex();
            var indices = new ArrayList<Integer>();
            var positions = new ArrayList<Vector3f>();
            var uvs = new ArrayList<Vector2f>();
            var normals = new ArrayList<Vector3f>();
            var tangents = new ArrayList<Vector3f>();
            var biTangents = new ArrayList<Vector3f>();
            var bones = new ArrayList<Bone>();

            // Indices
            var aiFaces = mesh.mFaces();
            for (var j = 0; j < mesh.mNumFaces(); j++) {
                var aiFace = aiFaces.get(j);
                IntBuffer pIndices = aiFace.mIndices();
                indices.add(pIndices.get(0));
                indices.add(pIndices.get(1));
                indices.add(pIndices.get(2));
            }

            // Positions
            var aiVert = mesh.mVertices();
            for (var j = 0; j < mesh.mNumVertices(); j++)
                positions.add(new Vector3f(aiVert.get(j).x(), aiVert.get(j).y(), aiVert.get(j).z()));

            // UV's
            var aiUV = mesh.mTextureCoords(0);
            if (aiUV != null) while (aiUV.remaining() > 0) {
                var uv = aiUV.get();
                uvs.add(new Vector2f(uv.x(), 1 - uv.y()));
            }

            // Normals
            var aiNormals = mesh.mNormals();
            if (aiNormals != null) for (var j = 0; j < mesh.mNumVertices(); j++)
                normals.add(new Vector3f(aiNormals.get(j).x(), aiNormals.get(j).y(), aiNormals.get(j).z()));

            // Tangents
            var aiTangents = mesh.mTangents();
            if (aiTangents != null) for (var j = 0; j < mesh.mNumVertices(); j++)
                tangents.add(new Vector3f(aiTangents.get(j).x(), aiTangents.get(j).y(), aiTangents.get(j).z()));

            // Bi-Tangents
            var aiBiTangents = mesh.mBitangents();
            if (aiBiTangents != null) for (var j = 0; j < mesh.mNumVertices(); j++)
                biTangents.add(new Vector3f(aiBiTangents.get(j).x(), aiBiTangents.get(j).y(), aiBiTangents.get(j).z()));

            // Bones
            if (mesh.mBones() != null) {
                var aiBones = requireNonNull(mesh.mBones());

                for (var j = 0; j < aiBones.capacity(); j++) {
                    var aiBone = AIBone.create(aiBones.get(j));
                    var bone = Bone.from(aiBone);
                    bones.add(bone);
                    boneMap.put(bone.name, bone);
                }
            }

            skeleton.store(bones.toArray(Bone[]::new));
            meshes[i] = new Mesh(name, material, indices, positions, uvs, normals, tangents, biTangents, bones);
        }

        skeleton.calculateBoneData();
        return meshes;
    }

    private static ARGBCpuTexture[] readTextureData(AIScene scene) {
        var textures = new ARGBCpuTexture[scene.mNumTextures()];

        for (int i = 0; i < scene.mNumTextures(); i++) {
            try (var stack = MemoryStack.stackPush()) {
                var aiTex = AITexture.create(scene.mTextures().get(i));
                var pWidth = stack.mallocInt(1);
                var pHeight = stack.mallocInt(1);
                var pChannels = stack.mallocInt(1);

                var pixels = STBImage.stbi_load_from_memory(aiTex.pcDataCompressed(), pWidth, pHeight, pChannels, 4);
                textures[i] = new ARGBCpuTexture(pixels, pWidth.get(0), pHeight.get(0));
            }
        }

        return textures;
    }

    private static ModelData.Material[] readMaterialData(AIScene scene) {
        var materials = new ModelData.Material[scene.mNumMaterials()];

        for (var i = 0; i < scene.mNumMaterials(); i++) {
            var aiMat = AIMaterial.create(scene.mMaterials().get(i));
            var matName = "material" + i;

            for (var j = 0; j < aiMat.mNumProperties(); j++) {
                var property = AIMaterialProperty.create(aiMat.mProperties().get(j));
                var name = property.mKey().dataString();
                var data = property.mData();
                if (name.equals(Assimp.AI_MATKEY_NAME))
                    matName = AIString.create(MemoryUtil.memAddress(data)).dataString();
            }

            try (var stack = MemoryStack.stackPush()) {
                var pDiffuse = AIString.calloc(stack);
                Assimp.aiGetMaterialTexture(aiMat, Assimp.aiTextureType_DIFFUSE, 0, pDiffuse, (IntBuffer) null, null, null, null, null, null);
                var pNormal = AIString.calloc(stack);
                Assimp.aiGetMaterialTexture(aiMat, Assimp.aiTextureType_NORMALS, 0, pDiffuse, (IntBuffer) null, null, null, null, null, null);
                materials[i] = new ModelData.Material(matName, pDiffuse.dataString(), pNormal.dataString(), "", new Vector4f(), 0, 0);
            }
        }

        return materials;
    }
}
