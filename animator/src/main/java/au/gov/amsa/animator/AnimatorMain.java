package au.gov.amsa.animator;

public class AnimatorMain {

    public static void main(String[] args) throws Exception {
        System.setProperty("http.proxyHost", "proxy.amsa.gov.au");
        System.setProperty("http.proxyPort", "8080");
        System.setProperty("https.proxyHost", "proxy.amsa.gov.au");
        System.setProperty("https.proxyPort", "8080");
        new Animator(Map.createMap(), new ModelManyCraft(), new ViewRecentTracks()).start();
    }

}
