package com.zidi.CodeRacer.vehicle.commands;


import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;

/**
 * VehicleCommand 定义了所有车辆动作命令的统一接口。
 * 每个命令在每一帧都会被 update 一次。
 */
public interface VehicleCommand {

    /**
     * 每帧执行命令逻辑。
     *
     * @param deltaTime   当前帧时间步长（秒）
     * @param context     车辆控制上下文（用于施加控制和读取状态）
     * @return true       表示该命令已完成（CommandRunner 会移除它）
     *         false      表示命令仍在执行中
     */
    boolean execute(float deltaTime, VehicleContext context);

    /**
     * （可选）命令开始执行时的回调。
     * 比如你可以用来打印日志或初始化内部计时器。
     */
    default void onStart(VehicleContext context) {}

    /**
     * （可选）命令结束时的回调。
     * 可以用于做收尾工作（例如把油门归零、打滑修正等）。
     */
    default void onEnd(VehicleContext context) {}
}
