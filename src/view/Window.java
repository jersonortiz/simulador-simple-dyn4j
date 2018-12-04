/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package view;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferStrategy;
import java.util.ArrayList;
import java.util.List;
import model.util.GameObject;
import model.util.Graphics2DRenderer;
import model.util.SimulationBody;
import org.dyn4j.dynamics.BodyFixture;
import org.dyn4j.dynamics.DetectResult;
import org.dyn4j.dynamics.World;
import org.dyn4j.geometry.Circle;
import org.dyn4j.geometry.Convex;
import org.dyn4j.geometry.Geometry;
import org.dyn4j.geometry.MassType;
import org.dyn4j.geometry.Mass;
import org.dyn4j.geometry.Rectangle;
import org.dyn4j.geometry.Transform;
import org.dyn4j.geometry.Vector2;

/**
 *
 * @author jerson
 *
 * @version 1.2.0
 *
 */
public final class Window extends javax.swing.JFrame {

    /**
     * The pixels per meter scale factor
     */
    public static final double SCALE = 45.0;
    /**
     * The conversion factor from nano to base
     */
    public static final double NANO_TO_BASE = 1.0e9;
    /**
     * The pixels per meter scale factor
     */
    protected double scale;
    /**
     * un objeto
     */
    private SimulationBody circle;
    /**
     * The dynamics engine
     */
    protected World world;
    /**
     * True if the simulation is exited
     */
    protected boolean stopped;
    /**
     * The time stamp for the last iteration
     */
    protected long last;
    /**
     * indica si es posible seleccionar objetos en pantalla
     *
     */
    private boolean seleccionable;
    /**
     * indica si es posible modificar manualmente los objetos en pantalla
     */
    private boolean editable;

    /**
     * True if the simulation is paused
     */
    private boolean paused;

    /**
     * the selected objet
     */
    SimulationBody selected;
    /**
     * objeto a mover
     */
    SimulationBody movible;

    /**
     * A point for tracking the mouse click
     */
    private Point point;

    /**
     * The picking radius
     */
    private static final double PICKING_RADIUS = 0.1;

    /**
     * The world space mouse point
     */
    private Vector2 worldPoint = new Vector2();

    /**
     * The picking results
     */
    private List<DetectResult> results = new ArrayList<DetectResult>();

    /**
     * ventana de configuracion del objeto seleccionado
     */
    objConfig obj;

    /**
     * A custom mouse adapter for listening for mouse clicks.
     *
     * @author jerson ortiz
     * @version 3.2.0
     * @since 3.2.0
     */
    private final class CustomMouseAdapter extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            point = new Point(e.getX(), e.getY());
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            point = null;
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            // store the mouse click postion for use later
            point = new Point(e.getX(), e.getY());
            super.mouseDragged(e);
        }

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {

        }

    }

    public Window() {
        initComponents();
        this.obj = new objConfig();
        this.scale = Window.SCALE;
        this.editable = false;
        MouseAdapter ml = new CustomMouseAdapter();
        this.canvas1.addMouseMotionListener(ml);
        this.canvas1.addMouseWheelListener(ml);
        this.canvas1.addMouseListener(ml);
        this.initializeWorld();
    }

    /**
     * Creates game objects and adds them to the world.
     */
    protected void initializeWorld() {

        this.world = new World();

        // this.world.setGravity(World.EARTH_GRAVITY);
        this.world.setGravity(World.ZERO_GRAVITY);

        //desactiva el autosleep de ese m
        this.world.getSettings().setAutoSleepingEnabled(false);

        Rectangle floorRect = new Rectangle(15.0, 1.0);
        SimulationBody floor = new SimulationBody();
        floor.addFixture(new BodyFixture(floorRect));
        floor.setMass(MassType.INFINITE);

        floor.translate(0.0, -2.0);
        this.world.addBody(floor);

        Circle cirShape = new Circle(0.5);
        this.circle = new SimulationBody();
        circle.addFixture(cirShape);
        circle.setMass(MassType.NORMAL);
        circle.translate(-4.0, 2.0);
        Vector2 v = circle.getMass().getCenter();
        double i = circle.getMass().getInertia();
        Mass m = new Mass(v, 1.0, i);
        circle.setMass(m);
        System.out.println(circle.getMass().getMass());

        //  circle.setLinearVelocity(new Vector2(5, 4.8));
        // test adding some force
        //   circle.applyForce(new Vector2(0.0, 9.8));
        // set some linear damping to simulate rolling friction
        // circle.setLinearDamping(0.05);
        this.world.addBody(circle);

    }

    /**
     * Start active rendering the simulation.
     * <p>
     * This should be called after the JFrame has been shown.
     */
    public void start() {
        this.stopped = false;

        this.last = System.nanoTime();
        this.canvas1.setIgnoreRepaint(true);
        this.canvas1.createBufferStrategy(2);
        Thread thread = new Thread() {
            public void run() {
                while (!isStopped()) {
                    gameLoop();

                }
            }
        };
        thread.setDaemon(true);
        thread.start();
    }

    /**
     * The method calling the necessary methods to update the game, graphics,
     * and poll for input.
     */
    protected void gameLoop() {

        Graphics2D g = (Graphics2D) this.canvas1.getBufferStrategy().getDrawGraphics();

        this.transform(g);
        this.clear(g);

        long time = System.nanoTime();
        long diff = time - this.last;
        this.last = time;
        double elapsedTime = diff / NANO_TO_BASE;

        this.render(g, elapsedTime);

        if (!paused) {
            //    System.out.println(""+this.paused);
            this.update(g, elapsedTime);
        }

        g.dispose();

        BufferStrategy strategy = this.canvas1.getBufferStrategy();

        if (!strategy.contentsLost()) {
            strategy.show();
        }
        Toolkit.getDefaultToolkit().sync();
    }

    /**
     * Performs any transformations to the graphics.
     * <p>
     * By default, this method puts the origin (0,0) in the center of the window
     * and points the positive y-axis pointing up.
     *
     * @param g the graphics object to render to
     */
    protected void transform(Graphics2D g) {
        final int w = this.canvas1.getWidth();
        final int h = this.canvas1.getHeight();
        AffineTransform yFlip = AffineTransform.getScaleInstance(1, -1);
        AffineTransform move = AffineTransform.getTranslateInstance(w / 2, -h / 2);
        g.transform(yFlip);
        g.transform(move);
    }

    /**
     * Clears the previous frame.
     *
     * @param g the graphics object to render to
     */
    protected void clear(Graphics2D g) {
        final int w = this.canvas1.getWidth();
        final int h = this.canvas1.getHeight();
        g.setColor(Color.WHITE);
        g.fillRect(-w / 2, -h / 2, w, h);
    }

    /**
     * Updates the world.
     *
     * @param g the graphics object to render to
     * @param elapsedTime the elapsed time from the last update
     */
    protected void update(Graphics2D g, double elapsedTime) {
        this.world.update(elapsedTime);
        if (this.editable) {
            this.addObjet();
        }

        this.seleccionar();
        this.mover();
        this.updateLabels();

    }

    /*
    mueve el objeto seleccionado al punto en que esta el mouse
     */
    protected void mover() {
        if (this.selected != null & this.point != null) {
            // convert from screen space to world space
            double x = (this.point.getX() - this.canvas1.getWidth() / 2.0) / this.scale;
            double y = -(this.point.getY() - this.canvas1.getHeight() / 2.0) / this.scale;

            // reset the transform of the controller body
            Transform tx = new Transform();
            tx.translate(x, y);
            this.selected.setTransform(tx);
            this.point = null;
        } else {
            this.selected = null;
        }
    }

    /*
    añade un objeto
     */
    protected void addObjet() {
        if (this.point != null) {
            double x = (this.point.getX() - this.canvas1.getWidth() / 2.0) / this.SCALE;
            double y = -(this.point.getY() - this.canvas1.getHeight() / 2.0) / this.SCALE;

            SimulationBody no = new SimulationBody();
            no.addFixture(Geometry.createSquare(0.5));
            no.translate(x, y);
            no.setMass(MassType.NORMAL);
            this.world.addBody(no);
            this.point = null;
        }
    }

    /*
    permite seleccionar un objeto de la pantalla
     */
    protected void seleccionar() {

        final double scale = Window.SCALE;
        this.results.clear();

        // we are going to use a circle to do our picking
        Convex convex = Geometry.createCircle(Window.PICKING_RADIUS);
        Transform transform = new Transform();
        double x = 0;
        double y = 0;

        // convert the point from panel space to world space
        if (this.point != null) {
            // convert the screen space point to world space
            x = (this.point.getX() - this.canvas1.getWidth() * 0.5) / scale;
            y = -(this.point.getY() - this.canvas1.getHeight() * 0.5) / scale;
            this.worldPoint.set(x, y);

            // set the transform
            transform.translate(x, y);

            // detect bodies under the mouse pointer
            this.world.detect(
                    convex,
                    transform,
                    null, // no, don't filter anything using the Filters 
                    false, // include sensor fixtures 
                    false, // include inactive bodies
                    false, // we don't need collision info 
                    this.results);

            // you could also iterate over the bodies and do a point in body test
//			for (int i = 0; i < this.world.getBodyCount(); i++) {
//				Body b = this.world.getBody(i);
//				if (b.contains(new Vector2(x, y))) {
//					// record this body
//				}
//			}
        }
    }

    /**
     * Renders the example.
     *
     * @param g the graphics object to render to
     * @param elapsedTime the elapsed time from the last update
     */
    protected void render(Graphics2D g, double elapsedTime) {

        g.setRenderingHint(RenderingHints.KEY_STROKE_CONTROL, RenderingHints.VALUE_STROKE_PURE);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // draw all the objects in the world
        for (int i = 0; i < this.world.getBodyCount(); i++) {
            // get the object
            SimulationBody body = (SimulationBody) this.world.getBody(i);
            this.render(g, elapsedTime, body);
        }

        if (this.point != null) {
            AffineTransform tx = g.getTransform();
            g.translate(this.worldPoint.x * Window.SCALE, this.worldPoint.y * Window.SCALE);
            Graphics2DRenderer.render(g, Geometry.createCircle(Window.PICKING_RADIUS), Window.SCALE, Color.GREEN);
            g.setTransform(tx);
        }

    }

    /**
     * Renders the body.
     *
     * @param g the graphics object to render to
     * @param elapsedTime the elapsed time from the last update
     * @param body the body to render
     */
    protected void render(Graphics2D g, double elapsedTime, SimulationBody body) {
        // draw the object
        // body.render(g, Window.SCALE);

        Color color = body.getColor();

        // change the color of the shape if its been picked
        for (DetectResult result : this.results) {
            SimulationBody sbr = (SimulationBody) result.getBody();
            if (sbr == body) {
                this.selected = body;
                this.movible = body;
                this.obj.setBody(body);
                color = Color.MAGENTA;
                break;
            }
        }

        // draw the object
        body.render(g, Window.SCALE, color);

    }

    /**
     * Stops the simulation.
     */
    public synchronized void stop() {
        this.stopped = true;
    }

    /**
     * Returns true if the simulation is stopped.
     *
     * @return boolean true if stopped
     */
    public boolean isStopped() {
        return this.stopped;
    }

    /**
     * Pauses the simulation.
     */
    public synchronized void pause() {
        this.paused = true;
    }

    /**
     * Pauses the simulation.
     */
    public synchronized void resume() {
        this.paused = false;
    }

    /**
     * Returns true if the simulation is paused.
     *
     * @return boolean true if paused
     */
    public boolean isPaused() {
        return this.paused;
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        canvas1 = new java.awt.Canvas();
        jCheckBox1 = new javax.swing.JCheckBox();
        jComboBox1 = new javax.swing.JComboBox<>();
        jLabel1 = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        jLabel7 = new javax.swing.JLabel();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel10 = new javax.swing.JLabel();
        jLabel11 = new javax.swing.JLabel();
        jButton2 = new javax.swing.JButton();
        jLabel12 = new javax.swing.JLabel();
        jLabel13 = new javax.swing.JLabel();
        jLabel14 = new javax.swing.JLabel();
        jLabel15 = new javax.swing.JLabel();
        jButton5 = new javax.swing.JButton();
        jButton6 = new javax.swing.JButton();
        jCheckBox2 = new javax.swing.JCheckBox();
        jCheckBox3 = new javax.swing.JCheckBox();
        jButton1 = new javax.swing.JButton();
        jButton3 = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.EXIT_ON_CLOSE);
        setResizable(false);

        jCheckBox1.setText("Activar Gravedad");
        jCheckBox1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox1ActionPerformed(evt);
            }
        });

        jComboBox1.setModel(new javax.swing.DefaultComboBoxModel<>(new String[] { "Item 1", "Item 2", "Item 3", "Item 4" }));

        jLabel1.setText("Escala");

        jLabel2.setText("Objeto seleccionado");

        jLabel3.setText("Velocidad eje x: ");

        jLabel4.setText("Velocidad eje y:");

        jLabel6.setText("Posicion eje x:");

        jLabel7.setText("Posicion eje y:");

        jLabel8.setText("0");

        jLabel9.setText("0");

        jLabel10.setText("Aceleracion eje x:");

        jLabel11.setText("Aceleracon eje y:");

        jButton2.setText("Iniciar");
        jButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton2ActionPerformed(evt);
            }
        });

        jLabel12.setText("0");

        jLabel13.setText("0");

        jLabel14.setText("0");

        jLabel15.setText("0");

        jButton5.setText("Pausar/Reanudar");
        jButton5.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton5ActionPerformed(evt);
            }
        });

        jButton6.setText("Reset");
        jButton6.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton6ActionPerformed(evt);
            }
        });

        jCheckBox2.setText("Añadir objetos");
        jCheckBox2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jCheckBox2ActionPerformed(evt);
            }
        });

        jCheckBox3.setText("Mover objetos");

        jButton1.setText("Tipo");

        jButton3.setText("jButton3");
        jButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                jButton3ActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(canvas1, javax.swing.GroupLayout.PREFERRED_SIZE, 600, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel5)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jLabel1)
                                .addGap(33, 33, 33)
                                .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(jLabel2, javax.swing.GroupLayout.PREFERRED_SIZE, 130, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addGroup(layout.createSequentialGroup()
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                                    .addComponent(jCheckBox1, javax.swing.GroupLayout.Alignment.LEADING)
                                    .addGroup(javax.swing.GroupLayout.Alignment.LEADING, layout.createSequentialGroup()
                                        .addGap(7, 7, 7)
                                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                            .addComponent(jLabel6)
                                            .addComponent(jLabel7)
                                            .addComponent(jLabel10)
                                            .addComponent(jLabel11)
                                            .addGroup(layout.createSequentialGroup()
                                                .addGap(1, 1, 1)
                                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                                                    .addComponent(jLabel4)
                                                    .addComponent(jLabel3)))
                                            .addGroup(layout.createSequentialGroup()
                                                .addGap(12, 12, 12)
                                                .addComponent(jButton3)))))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                                    .addComponent(jLabel9, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel12, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel13, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel14, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel15, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                                    .addComponent(jLabel8, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                            .addComponent(jCheckBox3)
                            .addGroup(layout.createSequentialGroup()
                                .addComponent(jCheckBox2)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 69, Short.MAX_VALUE)
                                .addComponent(jButton1))))
                    .addGroup(layout.createSequentialGroup()
                        .addComponent(jButton2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton5)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jButton6)))
                .addGap(29, 29, 29))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(layout.createSequentialGroup()
                        .addContainerGap()
                        .addComponent(canvas1, javax.swing.GroupLayout.PREFERRED_SIZE, 480, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(layout.createSequentialGroup()
                        .addGap(14, 14, 14)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jComboBox1, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel1))
                        .addGap(18, 18, 18)
                        .addComponent(jCheckBox1)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jCheckBox2)
                            .addComponent(jButton1))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(jCheckBox3)
                        .addGap(11, 11, 11)
                        .addComponent(jLabel2)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel3)
                            .addComponent(jLabel8))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel4, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel9))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(jLabel12))
                        .addGap(14, 14, 14)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel7)
                            .addComponent(jLabel13))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel10)
                            .addComponent(jLabel14))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel11)
                            .addComponent(jLabel15))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(jLabel5)
                        .addGap(9, 9, 9)
                        .addComponent(jButton3)))
                .addGap(19, 19, 19)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jButton2)
                    .addComponent(jButton5)
                    .addComponent(jButton6))
                .addContainerGap(20, Short.MAX_VALUE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents
/*
    activa y desactiva la gravedad
     */
    private void jCheckBox1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox1ActionPerformed
        if (this.jCheckBox1.isSelected()) {
            this.world.setGravity(World.EARTH_GRAVITY);
        } else {
            this.world.setGravity(World.ZERO_GRAVITY);
        }
    }//GEN-LAST:event_jCheckBox1ActionPerformed

    private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton2ActionPerformed
        /*
        inicia la simulacion
         */
        this.start();
    }//GEN-LAST:event_jButton2ActionPerformed
    /*
    pausa la simulacion
     */
    private void jButton5ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton5ActionPerformed
        if (isPaused()) {
            this.resume();
        } else {
            this.pause();
        }
    }//GEN-LAST:event_jButton5ActionPerformed
    /*
    reinicia la simulacion
     */
    private void jButton6ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton6ActionPerformed
        this.pause();
        this.stop();
        this.initializeWorld();
        this.start();
    }//GEN-LAST:event_jButton6ActionPerformed

    /*
    habilita la opcion de añadir o remover objetos
     */
    private void jCheckBox2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jCheckBox2ActionPerformed
        if (this.jCheckBox2.isSelected()) {
            this.editable = true;
        } else {
            this.editable = false;
        }
    }//GEN-LAST:event_jCheckBox2ActionPerformed

    private void jButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_jButton3ActionPerformed
        this.obj.setVisible(true);
    }//GEN-LAST:event_jButton3ActionPerformed

    public void updateLabels() {

        if (this.movible != null) {
            Vector2 v = this.movible.getLinearVelocity();

            this.jLabel8.setText("" + (float) v.x);
            this.jLabel9.setText("" + (float) v.y);

            Vector2 a = new Vector2(0.0, 0.0);
            //Vector2 p = circle.getLocalPoint(a);
            Vector2 p = movible.getWorldCenter();

            this.jLabel12.setText("" + (float) p.x);
            this.jLabel13.setText("" + (float) p.y);
            //System.out.println();
        }
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        /* Set the Nimbus look and feel */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /* If Nimbus (introduced in Java SE 6) is not available, stay with the default look and feel.
         * For details see http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html 
         */
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException ex) {
            java.util.logging.Logger.getLogger(Window.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (InstantiationException ex) {
            java.util.logging.Logger.getLogger(Window.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            java.util.logging.Logger.getLogger(Window.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        } catch (javax.swing.UnsupportedLookAndFeelException ex) {
            java.util.logging.Logger.getLogger(Window.class.getName()).log(java.util.logging.Level.SEVERE, null, ex);
        }
        //</editor-fold>

        /* Create and display the form */
        java.awt.EventQueue.invokeLater(new Runnable() {
            public void run() {
                Window w = new Window();
                w.setVisible(true);
                w.start();

            }
        });
    }

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private java.awt.Canvas canvas1;
    private javax.swing.JButton jButton1;
    private javax.swing.JButton jButton2;
    private javax.swing.JButton jButton3;
    private javax.swing.JButton jButton5;
    private javax.swing.JButton jButton6;
    private javax.swing.JCheckBox jCheckBox1;
    private javax.swing.JCheckBox jCheckBox2;
    private javax.swing.JCheckBox jCheckBox3;
    private javax.swing.JComboBox<String> jComboBox1;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    // End of variables declaration//GEN-END:variables
}
