package main;

import Space.*;
import javax.swing.*;
import java.awt.*;
import java.util.*;
import java.util.List;

public class GameFrame extends JPanel implements Runnable {

    // Screen settings
    private final int initTileSize = 16; //16x16 tile
    private final int scale = 4;
    public final int maxScreenCol = 16;
    public final int maxScreenRow = 10;
    public final int tileSize = initTileSize * scale; //64x64 tile
    public final int screenWidth = tileSize* maxScreenCol; //1024px
    public final int screenHeight = tileSize * maxScreenRow; //640px
    public final int FPS = 60; //60 frames per second

    // background
    BackgroundSlideshow slideshow = new BackgroundSlideshow(this);

    // game sound
    GameSound gs = new GameSound();

    // initialize KeyHandler
    KeyHandler keyH = new KeyHandler();



    // spaceship
    public Spaceship ship = new Spaceship(0, screenHeight / 2 - tileSize, tileSize,
                            tileSize, 999, 999, 8, keyH, this);

    // UFO
    public final List<Ufo> ufo = new ArrayList<>();

    // asteroids
    public final List<Asteroid> asteroids = new ArrayList<>();

    // rocket
    public final List<Rocket> rockets = new ArrayList<>();

    // bomb
    public final List<Bomb> bombs = new ArrayList<>();

    Thread gameThread;

    GameFrame() {

        this.setPreferredSize(new Dimension(screenWidth, screenHeight));
        this.setBackground(Color.BLACK);
        this.setDoubleBuffered(true);
        this.addKeyListener(keyH);
        this.setFocusable(true);

    }

    public void startGame() {

        gameThread = new Thread(this);
        gameThread.start();
    }

    public void update() {

        ship.move();

        for (SpaceObjects object: getObjects()) {

            object.move();
        }
    }

    public List<SpaceObjects> getObjects() {

        ArrayList<SpaceObjects> list = new ArrayList<>();
        list.addAll(asteroids);
        list.addAll(rockets);
        list.addAll(bombs);
        list.addAll(ufo);
        return list;
    }



    public void asteroidSpawn(){

       Random randomAsteroid = new Random();
       int random100 = randomAsteroid.nextInt(250);

       if (random100 == 0) {
           asteroids.add(new Asteroid(screenWidth, randomAsteroid.nextInt(screenHeight - tileSize), tileSize, tileSize, 333, 333, 1));
       }
    }

    // change the spawn direction to Y
    // TO-DO
    public void ufoSpawn() {

        if (ufo.size() > 0) return;

        Random randomUfo = new Random();
        double spawnProbability = 0.0050;
        // 0.50% chance of spawn in each iteration
        // 5 spawn in every 1000 iteration
        if (randomUfo.nextDouble() < spawnProbability) {

            ufo.add(new Ufo(screenWidth - tileSize * 2, (int)(Math.random() * screenHeight/2),tileSize, tileSize, 555, 333, 2, this));
            ufo.add(new Ufo(screenWidth - tileSize * 2, (int)(Math.random() * screenHeight/2 + tileSize),tileSize, tileSize, 555, 333, 2, this));
            ufo.add(new Ufo(screenWidth - tileSize * 2, (int)(Math.random() * screenHeight/2 + (tileSize * 4)),tileSize, tileSize, 555, 333, 2, this));
        }
    }

    public void rocketSpawn() {

        // rocket X
        if (keyH.rocketFireX && keyH.rocketFiredX) {

            ship.rocketType = "x";
            ship.fire();
            keyH.resetRocketX();
        }

        // rocket Y
        keyH.setCooldownDuration(1000);
        long currentTime = System.currentTimeMillis();

        if (keyH.rocketFireY) {
            // check if rocketY is not on cooldown or enough time has passed
            if (!keyH.yOnCooldown) {
                ship.rocketType = "y";
                ship.fire();
                keyH.yOnCooldown = true;
                keyH.lastYFireTime = currentTime;
            }
            // Optional: add an else condition to provide feedback that rocketY is on cooldown.
            // will add cooldown animation bar later
        }
        // reset cooldown flag
        if (keyH.yOnCooldown && currentTime - keyH.lastYFireTime >= keyH.cooldownDuration) {
            keyH.yOnCooldown = false;
        }
    }

    public void checkRockets() {

        for (Rocket rocket: rockets) {
            // check collision between rocket and asteroid
            for (Asteroid asteroid: asteroids) {

                if (asteroid.intersects(rocket)) {
                    asteroid.takeDamage(rocket.getDamage());
                    rocket.setDead(true);
                }
            }
            // check collision between rocket and UFO
            for (Ufo ufo: ufo) {

                if (ufo.intersects(rocket)) {
                    ufo.takeDamage(rocket.getDamage());
                    rocket.setDead(true);
                }
            }
            // check collision between rocket and bomb
            for (Bomb bomb: bombs) {

                if (bomb.intersects(rocket)) {
                    bomb.takeDamage(rocket.getDamage());
                    rocket.setDead(true);
                }
            }
        }
    }

    public void checkBomb() {
        // check collision between bombs and the spaceship
        for (Bomb bomb: bombs) {

            if (ship.intersects(bomb)) {
                ship.takeDamage(bomb.getDamage());
                bomb.takeDamage(ship.getDamage());
            }
        }
    }


    public void borderCollision() {

        // prevent ship from going offscreen
        ship.checkCollision();

        // bounce off ufo from the screen edges
        for (Rocket rocket: rockets) {
            rocket.checkCollision();
        }
        for (Bomb bomb: bombs) {
            bomb.checkCollision();
        }
        for (Asteroid asteroid: asteroids) {
            asteroid.checkCollision();
        }
        for (Ufo ufo: ufo) {
            ufo.checkCollision();
        }
    }

    public void removeDead() {

        rockets.removeIf(SpaceObjects::isDead);
        asteroids.removeIf(SpaceObjects::isDead);
        ufo.removeIf(SpaceObjects::isDead);
        bombs.removeIf(SpaceObjects::isDead);
    }

    public void paintComponent(Graphics g) {

        super.paintComponent(g);
        Graphics2D g2D = ((Graphics2D) g);

        // draw background
        slideshow.draw(g2D);

        // draw spaceship
        ship.drawSpaceship(g2D);

        // draw other game objects
       for (SpaceObjects object: getObjects()) {

           try {
               object.draw(g2D);
           } catch (Exception e) {
               e.printStackTrace();
           }
       }

        // ensure pending graphics operations are completed
        Toolkit.getDefaultToolkit().sync();
        // dispose of the graphics context after all drawing operations
        g2D.dispose();
    }

    // main game loop
    @Override
    public void run() {

        double drawInterval = (double) 1000000000 / FPS;
        double delta = 0;
        long lastTime = System.nanoTime();
        long currentTime;

        while(gameThread != null) {

            currentTime = System.nanoTime();
            delta += (currentTime - lastTime) / drawInterval;
            lastTime = currentTime;

            if (delta >= 1) {

                rocketSpawn();// spawn rockets on key press
                asteroidSpawn();// spawn asteroid
                //ufoSpawn();// spawn ufo's
                checkRockets();// checks rocket collision
                checkBomb();// checks boom collision
                removeDead();// remove dead objects
                update();// update position and remove dead objects
                borderCollision();// checks screen collision
                repaint();// repaint the panel
                delta--;
            }
        }
    }

    public List<Rocket> getRockets() {
        return rockets;
    }
}
