package com.zidi.CodeRacer.vehicle.components.vehicleBuilder.build;

import com.zidi.CodeRacer.vehicle.components.engine.Engine;
import com.zidi.CodeRacer.vehicle.components.engine.EngineSpec;
import com.zidi.CodeRacer.vehicle.components.engine.Impl.SimpleEngineImpl;
import com.zidi.CodeRacer.vehicle.components.frame.Impl.WoodenFrame;
import com.zidi.CodeRacer.vehicle.components.frame.Pose;
import com.zidi.CodeRacer.vehicle.components.fuelTank.FuelTank;
import com.zidi.CodeRacer.vehicle.components.fuelTank.Impl.WoodenFuelTank;
import com.zidi.CodeRacer.vehicle.components.powertrain.SimpleGeartrain;
import com.zidi.CodeRacer.vehicle.components.wheel.Impl.WoodenWheel;
import com.zidi.CodeRacer.vehicle.components.wheel.Wheel;
import com.zidi.CodeRacer.vehicle.controller.Impl.ConstantThrottleBrain;
import com.zidi.CodeRacer.vehicle.controller.Impl.SimpleControlBus;
import com.zidi.CodeRacer.vehicle.runtime.Impl.VehicleUpdater;
import java.util.ArrayList;
import java.util.List;

/** 最小可运行装配：一台“木头车” */
public class MinimalVehicleBuilder {

    public static class BuiltVehicle {
        public final Pose pose;
        public final Engine engine;
        public final FuelTank tank;
        public final List<Wheel> wheels;
        public final SimpleControlBus controlBus;
        public final ConstantThrottleBrain brain;
        public final SimpleGeartrain geartrain;
        public final VehicleUpdater updater;
        public final float massKg;
        private BuiltVehicle(Pose pose, Engine engine, FuelTank tank, List<Wheel> wheels,
                             SimpleControlBus bus, ConstantThrottleBrain brain,
                             SimpleGeartrain gt, VehicleUpdater updater, float massKg) {
            this.pose = pose; this.engine = engine; this.tank = tank;
            this.wheels = wheels; this.controlBus = bus; this.brain = brain;
            this.geartrain = gt; this.updater = updater; this.massKg = massKg;
        }
    }

    /** 构建一台能跑的最小车（恒 0.3 油门） */
    public BuiltVehicle build(float startX, float startY, float headingRad){
        // 1) 车架/位姿与质量
        WoodenFrame frame = new WoodenFrame("frame.wood","WoodFrame","", 100, 10);
        Pose pose = frame.pose().set(startX, startY, headingRad, 0f);
        float massKg = 800f; // 先给常数，后面用各 Part 的 mass 叠加

        // 2) 油箱
        FuelTank tank = new WoodenFuelTank("tank.wood","WoodTank","", 10, 5);

        // 3) 发动机规格（匿名 spec，参数按你 SimpleEngineImpl 的模型给）
        EngineSpec spec = new EngineSpec() {
            public float idleRpm(){ return 900f; }
            public float redlineRpm(){ return 7000f; }
            public float peakTorqueNm(){ return 220f; }
            public float peakTorqueRpm(){ return 4000f; }
            public float inertia(){ return 0.25f; }
            public float fullThrottleFuelUnitsPerSec(){ return 1.2f; }
        };
        Engine engine = new SimpleEngineImpl("eng.simple","SimpleEngine","", 120, 1000, spec);
        engine.attachFuelTank(tank);

        // 4) 四个轮
        List<Wheel> wheels = new ArrayList<>(4);
        wheels.add(new WoodenWheel("w.fl","FrontLeft","", 12, 50));
        wheels.add(new WoodenWheel("w.fr","FrontRight","", 12, 50));
        wheels.add(new WoodenWheel("w.rl","RearLeft","", 12, 50));
        wheels.add(new WoodenWheel("w.rr","RearRight","", 12, 50));

        // 5) 控制总线 + 一个“恒油门”大脑
        SimpleControlBus bus = new SimpleControlBus();
        ConstantThrottleBrain brain = new ConstantThrottleBrain(bus, 0.30f, 0f, 0f);

        // 6) 简易直驱传动：把扭矩按驱动轮平均
        SimpleGeartrain geartrain = new SimpleGeartrain();
        geartrain.setCandidateWheels(wheels);

        // 7) Updater：固定帧顺序推进
        VehicleUpdater updater = new VehicleUpdater(pose, massKg, engine, wheels, bus, geartrain);

        return new BuiltVehicle(pose, engine, tank, wheels, bus, brain, geartrain, updater, massKg);
    }
}
