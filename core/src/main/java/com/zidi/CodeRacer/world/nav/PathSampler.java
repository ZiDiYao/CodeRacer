
package com.zidi.CodeRacer.world.nav;

import com.badlogic.gdx.math.Vector2;

public class PathSampler {
    public static int nearestIndex(Path p, Vector2 pos, int hint){
        int n=p.size(); int s=Math.max(0, Math.min(hint, n-1));
        int lo=Math.max(0, s-6), hi=Math.min(n-1, s+6);
        int best=s; float bd=pos.dst2(p.get(s));
        for(int i=lo;i<=hi;i++){ float d=pos.dst2(p.get(i)); if(d<bd){bd=d; best=i;} }
        return best;
    }
    public static int lookaheadIndex(Path p, int i, int steps){
        return Math.min(p.size()-1, i+steps);
    }
}
