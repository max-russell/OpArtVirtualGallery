//Having an abstract base class for the artwork, makes it much simpler to expand the sketch with more artworks. Each descendent
//class just needs to override the constructor and two other methods: 'makeGeometry', which places the geometry pertaining to the artwork in the gallery, and
//'doMovement' which controls how the camera settings animate when the art effect is triggered.

abstract class Artwork
{
  int x, y;
  int teleportX, teleportY;
  Direction facingDir;
  Direction teleportDir;
  protected PImage flatImage;
  
  public Artwork(int x, int y, Direction dir)
  {
    this.x = x;
    this.y = y;
    this.facingDir = dir;
    this.teleportX = x;
    this.teleportY = y;
    this.teleportDir = dir;
  }
  
  public abstract void makeGeometry(PGraphics g);
  
  public abstract void doMovement();
}
