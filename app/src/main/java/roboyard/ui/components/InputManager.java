package roboyard.ui.components;

/**
 * Created by Pierre on 17/01/2015.
 * TODO: add the possibility to send a chain of commands in one touch-and move event
 */
public class InputManager {

    private boolean up, move, down, back, eventOccurred;
    private float touchX, touchY, prevTouchX, prevTouchY;

    /*
     * Constructeur du manager d'entrées.
     */
    public InputManager(){
        this.up = false;
        this.down = false;
        this.move = false;
        this.back = false;
        this.eventOccurred = false;
        this.touchX = 0;
        this.touchY = 0;
        this.prevTouchX = 0;
        this.prevTouchX = 0;
    }

    /*
     * Met à jour la position (x, y) touchée sur l'écran
     * @param Nouvelle position x sur l'écran
     * @param Nouvelle position y sur l'écran
     */
    public void updateTouchPosition(float x, float y){
        this.prevTouchX = this.touchX;
        this.prevTouchY = this.touchY;
        this.touchX = x;
        this.touchY = y;
    }

    /*
     * Enclenche un événement lorsque l'écran n'est plus touché.
     * @param Position x sur l'écran
     * @param Position y sur l'écran
     */
    public void startUp(float x, float y){
        this.updateTouchPosition(x, y);
        this.eventOccurred = true;
        this.up = true;
    }

    /*
     * Enclenche un événement lorsque l'écran est touché.
     * @param Position x sur l'écran
     * @param Position y sur l'écran
     */
    public void startDown(float x, float y){
        this.updateTouchPosition(x, y);
        this.eventOccurred = true;
        this.down = true;
    }

    /*
     * Enclenche un événement lorsque qu'il y a un déplacement sur l'écran.
     * @param Position x sur l'écran
     * @param Position y sur l'écran
     */
    public void startMove(float x, float y){
        this.updateTouchPosition(x, y);
        this.eventOccurred = true;
        this.move = true;
    }

    /*
     * Enclenche un événement lorsque qu'il y a un appui sur la touche retour.
     */
    public void startBack(){
        this.eventOccurred = true;
        this.back = true;
    }

    /*
     * Réinitialise tous les événements.
     */
    public void resetEvents(){
        this.eventOccurred = false;
        this.up = false;
        this.down = false;
        this.move = false;
        this.back = false;
    }

    /*
     * Retourne la dernière position x touchée sur l'écran
     * @return Position x de l'écran
     */
    public float getTouchX(){
        return this.touchX;
    }

    /*
     * Retourne la dernière position y touchée sur l'écran
     * @return Position y de l'écran
     */
    public float getTouchY(){
        return this.touchY;
    }

    /*
     * Retourne la l'avant-dernière position x touchée sur l'écran
     * @return Avant-dernière position x de l'écran
     */
    public float getPrevTouchX(){
        return this.prevTouchX;
    }

    /*
     * Retourne la l'avant-dernière position y touchée sur l'écran
     * @return Avant-dernière position y de l'écran
     */
    public float getPrevTouchY(){
        return this.prevTouchY;
    }

    /*
     * Retourne vrai si un événement a eu lieu, faux sinon.
     * @return Vrai si un énénement s'est produit
     */
    public boolean eventHasOccurred(){
        return this.eventOccurred;
    }

    /*
     * Retourne vrai si un événement du type appui a eu lieu, faux sinon.
     * @return Vrai si l'énénement s'est produit
     */
    public boolean downOccurred(){
        return this.down;
    }

    /*
     * Retourne vrai si un événement du type déplacement a eu lieu, faux sinon.
     * @return Vrai si l'énénement s'est produit
     */
    public boolean moveOccurred(){
        return this.move;
    }

    /*
     * Retourne vrai si un événement du type fin d'appui a eu lieu, faux sinon.
     * @return Vrai si l'énénement s'est produit
     */
    public boolean upOccurred(){
        return this.up;
    }

    /*
     * Retourne vrai si un événement d'appui sur la touche retour a eu lieu.
     * @return Vrai si l'énénement s'est produit
     */
    public boolean backOccurred(){
        return this.back;
    }

    /**
     * Consumes the current event to prevent other UI elements from processing it.
     * This is particularly useful for accessibility features like TalkBack.
     */
    public void consumeEvent() {
        this.eventOccurred = false;
    }
}
