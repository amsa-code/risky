package au.gov.amsa.animator;

import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;

import rx.Observable;
import au.gov.amsa.risky.format.HasFix;

public class Animator {

    private Observable<HasFix> source;

    public Animator(Observable<HasFix> source) {
        this.source = source;
    }

    private void createFrame() {
        try {
            // Create and set up the window.
            JFrame frame = new JFrame("Animator");
            frame.setSize(500, 400);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);

            JLabel label = new JLabel("Animator");
            frame.getContentPane().add(label);

            JPanel panel = new JPanel();

            // // Display the window.
            // frame.pack();
            frame.setVisible(true);
            System.out.println("created frame");
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    public void show() {
        // Schedule a job for the event-dispatching thread:
        // creating and showing this application's GUI.
        EventQueue.invokeLater(() -> {
            createFrame();
        });
    }

    public void animate() {

    }

    public static void main(String[] args) {
        Animator animator = new Animator(null);
        animator.show();
    }

}
