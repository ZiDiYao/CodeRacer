package com.zidi.CodeRacer.vehicle.components.wheel.Impl;

import com.zidi.CodeRacer.vehicle.components.wheel.WheelSpec;

// 一个最简木头轮（只需提供 spec）
public class WoodenWheel extends DefaultWheel {
    private static final WheelSpec SPEC = new WheelSpec() {
        public float radius() { return 0.30f; }
        public float width() { return 0.12f; }
        public float maxSteerDeg() { return 35f; }
        public boolean driven() { return true; }
        public boolean braked() { return true; }
        public float muDry() { return 0.6f; }
        public float muWet() { return 0.35f; }
        public float cRolling() { return 0.015f; }
    };
    public WoodenWheel(String id, String name, String desc, int mass, int cost) {
        super(id, name, desc, mass, cost, SPEC);
    }
    @Override public void onClick() { /* 调试/UI */ }
}
