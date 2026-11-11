package entity;

import main.GamePanel;
import main.KeyHandler;

import java.awt.*;


public class player extends entity{

    GamePanel gp;
    KeyHandler KeyH;

    public player(GamePanel gp, KeyHandler keyH){
        this.gp = gp;
        this.KeyH = keyH;
        setDefaultValues();
    }
    public void setDefaultValues(){


        x = 100;
        y = 100;
        speed = 4;
    }

    public void update(){
        if(KeyH.upPressed == true) {
            x -= speed;
        }
        else if(KeyH.downPressed == true) {
            y += speed;
        }
        else if (KeyH.leftPressed == true) {
            x -= speed;

        }
        else if (KeyH.rightPressed == true) {
            x += speed;
        }
    }
    public void draw(Graphics2D g2){
        g2.setColor(Color.white);
        g2.fillRect(x,y, gp.tileSize, gp.tileSize);


    }



}
