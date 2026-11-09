#version 330

uniform sampler2D InSampler;

in vec2 texCoord;

layout(std140) uniform SamplerInfo {
    vec2 OutSize;
    vec2 InSize;
};

layout(std140) uniform AdjustConfig {
    // x = hue shift in degrees, y = saturation, z = brightness, w = contrast (all 1.0 = unchanged, hue 0.0)
    vec4 Params;
};

out vec4 fragColor;

void main() {
    vec3 c = texture(InSampler, texCoord).rgb;

    // hue rotation around the grey axis
    float a = radians(Params.x);
    float cs = cos(a);
    float sn = sin(a);
    const vec3 k = vec3(0.57735026);
    c = c * cs + cross(k, c) * sn + k * dot(k, c) * (1.0 - cs);

    float luma = dot(c, vec3(0.2126, 0.7152, 0.0722));
    c = mix(vec3(luma), c, Params.y);
    c *= Params.z;
    c = (c - 0.5) * Params.w + 0.5;

    fragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
}
