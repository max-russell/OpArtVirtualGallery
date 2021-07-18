class ArtFall extends Artwork
{
  public ArtFall(int x, int y, Direction dir)
  {
    super(x, y, dir);
    flatImage = createFallImage();
    teleportX = 2;
    teleportY = 3;
    teleportDir = Direction.WEST;
  }
  
  PImage createFallImage()
  {
    PGraphics g = createGraphics(SZ,SZ);
    g.beginDraw();
    makeFall(g, 0);
    g.endDraw();
    return g;
  }
  
  void doMovement()
  {
    forwardPositionModifier = -SZ + ((float)(ART_EFFECT_FRAMES - artEffectCounter) * (float)SZ/ART_EFFECT_FRAMES);
  }
  
  void makeFall(PGraphics g, float phase)
  {
    g.pushMatrix();
    
    //In the setup phase during pre-rendering a 3D renderer is not needed.
    if (g.is3D())
      g.translate(-HALF_SZ,-HALF_SZ, -HALF_SZ);
    
    g.scale(1.0/16,1.0/16);
    
    //The waves texture is drawn in 16 rows, the amount to stretch each row is determined with the 'sin' function to provide
    //a smooth wave effect. By tying the sine curve's phase to the sketch's animation counter, the waves seem to undulate 
    //across the screen with each frame.
    
    for(int y = 0; y < 16; y++)
    {
      float h = (sin(y / 16.0 * TWO_PI + phase) + 1.00);
      if (h < 0.1) h = 0.1;
      g.scale(1,  h);
      for(int x = 0; x < 16; x++)
      {
        g.image(textureWaves, SZ * x, 0);
      }
      g.translate(0,SZ);
      g.scale(1,1/h);
     
    } 
    g.popMatrix();
  }
  
  void makeGeometry(PGraphics g)
  {
    g.translate(SZ * teleportX, 0, SZ * teleportY);
    g.rotateY(-teleportDir.getAngle());
    
    g.translate(-SZ,0,-SZ);
    for(int n = 0; n < 3; n++)
    {
      makeFall(g, animateCounter / 25);
      g.translate(SZ, 0, 0);
    }
  }
}
