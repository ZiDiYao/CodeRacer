package com.zidi.CodeRacer.world.nav;

import java.util.List;
import com.badlogic.gdx.math.Vector2;


public class Path {
    public final List<Vector2> pts;
    public Path(List<Vector2> pts){ this.pts = pts; }
    public int size(){ return pts.size(); }
    public Vector2 get(int i){ return pts.get(i); }
}
