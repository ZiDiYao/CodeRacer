// com.zidi.CodeRacer.world.nav.PathBuilder
package com.zidi.CodeRacer.world.nav;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import java.util.ArrayList;
import java.util.List;

public final class PathBuilder {
    private PathBuilder(){}

    /** 均匀采样的折线 */
    public static Path polyline(Vector2... pts){
        return new Path(java.util.Arrays.asList(pts));
    }

    /** 圆弧（按角度步长采样）*/
    public static Path arc(Vector2 center, float radius, float startRad, float endRad, float stepRad){
        List<Vector2> pts = new ArrayList<>();
        float dir = Math.signum(endRad - startRad);
        stepRad = Math.abs(stepRad) * dir;
        for(float a=startRad; (dir>0? a<=endRad:a>=endRad); a+=stepRad){
            pts.add(new Vector2(center.x + radius*MathUtils.cos(a),
                center.y + radius*MathUtils.sin(a)));
        }
        pts.add(new Vector2(center.x + radius*MathUtils.cos(endRad),
            center.y + radius*MathUtils.sin(endRad)));
        return new Path(pts);
    }

    /** 三次贝塞尔采样 */
    public static Path cubicBezier(Vector2 p0, Vector2 p1, Vector2 p2, Vector2 p3, int samples){
        List<Vector2> pts = new ArrayList<>(samples+1);
        for(int i=0;i<=samples;i++){
            float t=i/(float)samples, u=1f-t;
            float x = u*u*u*p0.x + 3*u*u*t*p1.x + 3*u*t*t*p2.x + t*t*t*p3.x;
            float y = u*u*u*p0.y + 3*u*u*t*p1.y + 3*u*t*t*p2.y + t*t*t*p3.y;
            pts.add(new Vector2(x,y));
        }
        return new Path(pts);
    }

    /** 车道换道 S 曲线：从 (x,y,heading) 到平行车道，宽度 laneW，长度 len */
    public static Path laneChangeS(float x, float y, float headingRad, float laneW, float len){
        Vector2 p0 = new Vector2(x, y);
        Vector2 dir = new Vector2(MathUtils.cos(headingRad), MathUtils.sin(headingRad));
        Vector2 left= new Vector2(-dir.y, dir.x); // 左法向

        Vector2 p3 = new Vector2(x + len*dir.x + laneW*left.x,
            y + len*dir.y + laneW*left.y);
        // 控制点：沿着切线方向适当分配 1/3、2/3
        Vector2 p1 = new Vector2(x + (len*0.33f)*dir.x, y + (len*0.33f)*dir.y);
        Vector2 p2 = new Vector2(p3.x - (len*0.33f)*dir.x, p3.y - (len*0.33f)*dir.y);

        return cubicBezier(p0, p1, p2, p3, Math.max(10, (int)(len*3)));
    }
}
