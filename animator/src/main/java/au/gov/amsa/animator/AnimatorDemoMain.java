package au.gov.amsa.animator;

public class AnimatorDemoMain {

    public static void main(String[] args) {
        new Animator(Map.createMap(), new ModelEmpty(), new ViewRecentTracks()).start();
    }
}
