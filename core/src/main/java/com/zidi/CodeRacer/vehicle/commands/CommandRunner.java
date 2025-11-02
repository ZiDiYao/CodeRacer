package com.zidi.CodeRacer.vehicle.commands;

import com.zidi.CodeRacer.vehicle.runtime.VehicleContext;

import java.util.LinkedList;
import java.util.Queue;

public class CommandRunner {

    private final Queue<VehicleCommand> queue = new LinkedList<>();

    private VehicleCommand current = null;

    public void addCommand(VehicleCommand cmd) {
        queue.add(cmd);
    }

    public void update(float dt, VehicleContext ctx) {
        // 若没有当前命令且队列不为空，则取出一个
        if (current == null && !queue.isEmpty()) {
            current = queue.poll();
            current.onStart(ctx);
        }

        // 执行当前命令
        if (current != null) {
            boolean finished = current.execute(dt, ctx);
            if (finished) {
                current.onEnd(ctx);
                current = null; // 切换到下一个命令
            }
        }
    }

    /**
     * 判断命令队列是否已全部执行完毕。
     */
    public boolean isIdle() {
        return current == null && queue.isEmpty();
    }

    /**
     * 清空所有待执行命令。
     */
    public void clear() {
        queue.clear();
        current = null;
    }

    public VehicleCommand getCurrent() {
        return current;
    }

}
