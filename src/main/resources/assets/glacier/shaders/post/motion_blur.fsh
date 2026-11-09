#version 330
#extension GL_ARB_separate_shader_objects : require

uniform sampler2D InSampler;
uniform sampler2D PrevSampler;

layout(location = 0) in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform BlurConfig {
    vec4 Params;
};

layout(location = 0) out vec4 fragColor;

// Frame accumulation blur: blends the previous (already blurred) frame into the current one.
// Params.x = weight of the previous frame. The persistent target starts cleared (alpha 0), so the first frame is unblended.
void main() {
    vec4 cur = texture(InSampler, texCoord);
    vec4 prev = texture(PrevSampler, texCoord);
    float s = prev.a > 0.5 ? clamp(Params.x, 0.0, 0.97) : 0.0;
    fragColor = vec4(mix(cur.rgb, prev.rgb, s), 1.0);
}
