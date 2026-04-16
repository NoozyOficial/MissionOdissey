package com.noozy.missionodyssey.client.render;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import org.joml.Vector3f;

import java.io.InputStreamReader;
import java.util.*;

public class OdysseyModel {
    private static final Gson GSON = new Gson();

    public final Map<String, Bone> bones = new HashMap<>();
    public final Map<String, Animation> animations = new HashMap<>();
    public final Vector3f textureSize = new Vector3f(16, 16, 0);

    public static OdysseyModel load(ResourceLocation location) {
        try {
            Optional<Resource> resource = Minecraft.getInstance().getResourceManager().getResource(location);
            if (resource.isEmpty()) return null;

            JsonObject json = GSON.fromJson(new InputStreamReader(resource.get().open()), JsonObject.class);
            OdysseyModel model = new OdysseyModel();


            if (json.has("model") && json.getAsJsonObject("model").has("texture")) {
                JsonArray tex = json.getAsJsonObject("model").getAsJsonArray("texture");
                model.textureSize.set(tex.get(0).getAsFloat(), tex.get(1).getAsFloat(), 0);
            }


            if (json.has("model") && json.getAsJsonObject("model").has("groups")) {
                JsonObject groups = json.getAsJsonObject("model").getAsJsonObject("groups");
                for (Map.Entry<String, JsonElement> entry : groups.entrySet()) {
                    String name = entry.getKey();
                    JsonObject groupJson = entry.getValue().getAsJsonObject();
                    Bone bone = new Bone(name);

                    if (groupJson.has("origin")) {
                        JsonArray origin = groupJson.getAsJsonArray("origin");
                        bone.pivot.set(origin.get(0).getAsFloat(), origin.get(1).getAsFloat(), origin.get(2).getAsFloat());
                    }

                    if (groupJson.has("rotate")) {
                        JsonArray rotate = groupJson.getAsJsonArray("rotate");
                        bone.rotation.set(rotate.get(0).getAsFloat(), rotate.get(1).getAsFloat(), rotate.get(2).getAsFloat());
                    }

                    if (groupJson.has("parent")) {
                        bone.parentName = groupJson.get("parent").getAsString();
                    }

                    if (groupJson.has("cubes")) {
                        JsonArray cubes = groupJson.getAsJsonArray("cubes");
                        for (JsonElement cubeEl : cubes) {
                            JsonObject cubeJson = cubeEl.getAsJsonObject();
                            Cube cube = new Cube();

                            JsonArray from = cubeJson.getAsJsonArray("from");
                            cube.from.set(from.get(0).getAsFloat(), from.get(1).getAsFloat(), from.get(2).getAsFloat());

                            JsonArray size = cubeJson.getAsJsonArray("size");
                            cube.size.set(size.get(0).getAsFloat(), size.get(1).getAsFloat(), size.get(2).getAsFloat());

                            if (cubeJson.has("uvs")) {
                                JsonObject uvs = cubeJson.getAsJsonObject("uvs");
                                for (String face : uvs.keySet()) {
                                    JsonArray uvArr = uvs.getAsJsonArray(face);
                                    float[] uv = new float[4];
                                    for (int i = 0; i < 4; i++) uv[i] = uvArr.get(i).getAsFloat();


                                    String faceName = face;
                                    if (face.equals("front")) faceName = "south";
                                    else if (face.equals("back")) faceName = "north";
                                    else if (face.equals("left")) faceName = "west";
                                    else if (face.equals("right")) faceName = "east";

                                    cube.uvs.put(faceName, uv);
                                }
                            }
                            bone.cubes.add(cube);
                        }
                    }
                    model.bones.put(name, bone);
                }
            }


            if (json.has("animations")) {
                JsonObject anims = json.getAsJsonObject("animations");
                for (Map.Entry<String, JsonElement> entry : anims.entrySet()) {
                    String name = entry.getKey().toLowerCase();
                    JsonObject animJson = entry.getValue().getAsJsonObject();
                    Animation animation = new Animation();
                    animation.duration = animJson.has("duration") ? animJson.get("duration").getAsFloat() : 0;

                    if (animJson.has("groups")) {
                        JsonObject groupsJson = animJson.getAsJsonObject("groups");
                        for (Map.Entry<String, JsonElement> groupEntry : groupsJson.entrySet()) {
                            String groupName = groupEntry.getKey();
                            JsonObject groupAnim = groupEntry.getValue().getAsJsonObject();
                            BoneAnimation boneAnim = new BoneAnimation();

                            loadKeyframes(groupAnim, "translate", boneAnim.translateFrames);
                            loadKeyframes(groupAnim, "rotate", boneAnim.rotateFrames);
                            loadKeyframes(groupAnim, "scale", boneAnim.scaleFrames);

                            animation.boneAnimations.put(groupName, boneAnim);
                        }
                    }
                    model.animations.put(name, animation);
                }
            }

            return model;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private static void loadKeyframes(JsonObject groupAnim, String type, List<Keyframe> frames) {
        if (groupAnim.has(type)) {
            JsonArray arr = groupAnim.getAsJsonArray(type);
            for (JsonElement el : arr) {
                JsonArray kf = el.getAsJsonArray();
                frames.add(new Keyframe(
                    kf.get(0).getAsFloat(),
                    kf.get(1).getAsString(),
                    new Vector3f(kf.get(2).getAsFloat(), kf.get(3).getAsFloat(), kf.get(4).getAsFloat())
                ));
            }
            frames.sort(Comparator.comparingDouble(f -> f.time));
        }
    }

    public static class Bone {
        public final String name;
        public String parentName;
        public final Vector3f pivot = new Vector3f();
        public final Vector3f rotation = new Vector3f();
        public final List<Cube> cubes = new ArrayList<>();

        public Bone(String name) { this.name = name; }
    }

    public static class Cube {
        public final Vector3f from = new Vector3f();
        public final Vector3f size = new Vector3f();
        public final Map<String, float[]> uvs = new HashMap<>();
    }

    public static class Animation {
        public float duration;
        public final Map<String, BoneAnimation> boneAnimations = new HashMap<>();
    }

    public static class BoneAnimation {
        public final List<Keyframe> translateFrames = new ArrayList<>();
        public final List<Keyframe> rotateFrames = new ArrayList<>();
        public final List<Keyframe> scaleFrames = new ArrayList<>();
    }

    public static class Keyframe {
        public final float time;
        public final String interpolation;
        public final Vector3f value;

        public Keyframe(float time, String interpolation, Vector3f value) {
            this.time = time;
            this.interpolation = interpolation;
            this.value = value;
        }
    }
}
