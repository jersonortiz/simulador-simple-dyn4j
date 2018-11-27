/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package model.util;

import java.awt.Color;
import java.awt.Graphics2D;
import org.dyn4j.geometry.Convex;
import static view.Window.SCALE;
import org.dyn4j.dynamics.Body;
import org.dyn4j.dynamics.BodyFixture;
import java.awt.geom.AffineTransform;

/**
 *
 * @author jerson
 */
public final class GameObject extends Body {

    protected Color color;

    public GameObject() {

        this.color = new Color(
                (float) Math.random() * 0.5f + 0.5f,
                (float) Math.random() * 0.5f + 0.5f,
                (float) Math.random() * 0.5f + 0.5f);
    }

    public void render(Graphics2D g) {
        AffineTransform ot = g.getTransform();
        AffineTransform lt = new AffineTransform();
        lt.translate(this.transform.getTranslationX() * SCALE, this.transform.getTranslationY() * SCALE);
        lt.rotate(this.transform.getRotation());
        g.transform(lt);
        for (BodyFixture fixture : this.fixtures) {
            Convex convex = fixture.getShape();
            Graphics2DRenderer.render(g, convex, SCALE, color);
        }
        g.setTransform(ot);
    }
}
