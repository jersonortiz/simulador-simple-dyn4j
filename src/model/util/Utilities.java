/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.util;

import java.awt.Point;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Triangle;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Polygon;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.Vector2;

/**
 *
 * @author jerson
 */
public class Utilities {

    public static BodyFixture createBody(int tipo) {
        BodyFixture f = null;
        switch (tipo) {
            case 0:
                Circle cirShape = new Circle(0.5);
                f = new BodyFixture(cirShape);
                break;
            case 1:
                Rectangle r = Geometry.createSquare(0.5);
                f = new BodyFixture(r);
                break;
            case 2:
                Triangle tr = Geometry.createTriangle(new Vector2(0.0, 0.5), new Vector2(-0.5, -0.5), new Vector2(0.5, -0.5));
                f = new BodyFixture(tr);
                break;
            case 3:
                Polygon po = Geometry.createUnitCirclePolygon(5, 0.5);
                f = new BodyFixture(po);
                break;
            case 4:
                Polygon he = Geometry.createUnitCirclePolygon(6, 0.5);
                f = new BodyFixture(he);
                break;
        }
        return f;
    }

    public static Vector2 pixelToM(Point p, double width, double heigth, double scale) {
        double x = (p.getX() - width / 2.0) / scale;
        double y = -(p.getY() - heigth / 2.0) / scale;
        return new Vector2(x, y);

    }

}
