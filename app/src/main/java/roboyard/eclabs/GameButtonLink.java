package roboyard.eclabs;

public class GameButtonLink {

    private final int x;
    private final int y;
    private final int w;
    private final int h;
    private final String url;

    public GameButtonLink(int x, int y, int w, int h, String url) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
        this.url = url;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getW() {
        return w;
    }

    public int getH() {
        return h;
    }

    public String getUrl() {
        return url;
    }
}
