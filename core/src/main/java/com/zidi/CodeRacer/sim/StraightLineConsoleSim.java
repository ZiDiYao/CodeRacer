package com.zidi.CodeRacer.sim;

import com.zidi.CodeRacer.vehicle.components.vehicleBuilder.build.MinimalVehicleBuilder;
import com.zidi.CodeRacer.vehicle.components.vehicleBuilder.build.MinimalVehicleBuilder.BuiltVehicle;
import com.zidi.CodeRacer.vehicle.components.wheel.Wheel;

import java.util.Locale;

public class StraightLineConsoleSim {

    public static void main(String[] args) throws InterruptedException {
        // 1) 造车：起点(0,0)、朝向0（向x正方向）
        MinimalVehicleBuilder builder = new MinimalVehicleBuilder();
        BuiltVehicle car = builder.build(0f, 0f, 0f);

        // 2) 固定步长仿真 10 秒
        final double dt = 1.0 / 120.0;     // 120 Hz 物理帧
        final double printEvery = 0.1;     // 打印间隔 0.1s
        final double totalTime = 10.0;     // 总时长 10s

        double t = 0.0;
        double nextPrintAt = 0.0;

        // 打印表头
        System.out.println("time(s)\tx\ty\tspeed(m/s)\trpm\ttorque(Nm)\tfuel\tFx_sum");

        while (t < totalTime) {
            // 大脑把恒定控制写入总线（油门0.3、刹车0、转向0）
            car.brain.update();

            // 跑一帧物理
            car.updater.step((float) dt);

            t += dt;

            // 聚合四轮纵向力，定期输出
            if (t >= nextPrintAt) {
                float sumFx = 0f;
                for (Wheel w : car.wheels) sumFx += w.getFx();

                System.out.printf(Locale.US,
                    "%.2f\t%.2f\t%.2f\t%.2f\t%.0f\t%.0f\t%.1f\t%.1f%n",
                    t,
                    car.pose.getX(),
                    car.pose.getY(),
                    car.pose.getSpeed(),
                    car.engine.getRpm(),
                    car.engine.getTorqueNm(),
                    car.tank.getFuelLevel(),
                    sumFx
                );
                nextPrintAt += printEvery;
            }

            // 控制台版本不需要真睡眠；如果想看“实时滚动”，取消注释：
            // Thread.sleep(1);
        }

        // 结束时再汇总一行
        float sumFx = 0f;
        for (Wheel w : car.wheels) sumFx += w.getFx();
        System.out.printf(Locale.US,
            "END\t%.2f\t%.2f\t%.2f\t%.0f\t%.0f\t%.1f\t%.1f%n",
            car.pose.getX(),
            car.pose.getY(),
            car.pose.getSpeed(),
            car.engine.getRpm(),
            car.engine.getTorqueNm(),
            car.tank.getFuelLevel(),
            sumFx
        );
    }
}
