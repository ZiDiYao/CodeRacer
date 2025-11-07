package com.zidi.CodeRacer.vehicle.components.frame.Impl;

import com.zidi.CodeRacer.Commons.Enum.InstallSite;

import java.util.List;

// com.zidi.CodeRacer.vehicle.components.frame.Impl.WoodenFrame
import static com.badlogic.gdx.math.MathUtils.degreesToRadians;

public class WoodenFrame extends DefaultFrame {
    public WoodenFrame(String id, String name, String desc, int mass, int cost) {
        super(id, name, desc, mass, cost,
            /*maxDurability*/ 2,
            /*FRONT*/ 0.80f, 0.00f, 0f,
            /*LEFT */ 0.60f, 0.30f, 35f * degreesToRadians,
            /*RIGHT*/ 0.60f,-0.30f,-35f * degreesToRadians);
    }

    @Override public void onClick() {}
    @Override public void reset() {
        // 如需重置耐久与卸载，按需实现
    }
}
