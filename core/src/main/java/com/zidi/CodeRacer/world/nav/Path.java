package com.zidi.CodeRacer.world.nav;

import java.util.List;
import com.badlogic.gdx.math.Vector2;


public record Path(List<Vector2> pts) {
    public int size() {
        return pts.size();
    }

    public Vector2 get(int i) {
        return pts.get(i);
    }
}
