package main;

import javax.swing.JFrame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {
    public static void main(String[] args) {
        //TIP Press <shortcut actionId="ShowIntentionActions"/> with your caret at the highlighted text
        // to see how IntelliJ IDEA suggests fixing it.

        JFrame window = new JFrame();

        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        window.setResizable(false);
        window.setTitle("2D Adventure");

        GamePanel gamepanel = new GamePanel();
        window.add(gamepanel);

        // Keep keyboard input directed to the game panel when the window gains focus.
        window.addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowGainedFocus(WindowEvent e) {
                gamepanel.requestFocusInWindow();
            }
        });

        window.pack();

        window.setLocationRelativeTo(null);
        window.setVisible(true);
        gamepanel.requestFocusInWindow();

        gamepanel.startGameThread();


    }
}