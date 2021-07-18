class ArtHero extends Artwork
{
  int SEGMENTS = 26;
  
  //The size of the triangular segments of the artwork are hardcoded to roughly match Riley's original composition.
  float[] segmentProportionsBottom = {0.001, 0.054, 0.106, 0.168, 0.230, 0.296, 0.366, 0.442, 0.520, 0.606, 0.692, 0.788, 0.888, 1.0};
  float[] segmentProportionsTop = {0.001, 0.043, 0.121, 0.235, 0.388, 0.632, 0.694, 0.749, 0.799, 0.848, 0.895, 0.935, 0.973, 1.0};
  
  public ArtHero(int x, int y, Direction dir)
  {
    super(x, y, dir);
    flatImage = createHeroImage();
    teleportX = 10;
    teleportY = 13;
    teleportDir = Direction.WEST;
  }
  
  PImage createHeroImage()
  {
    PGraphics g = createGraphics(SZ,SZ,P3D);
    g.beginDraw();
    g.camera(0,0,0,0,0,-HALF_SZ,0,1,0);
    float cameraZ = HALF_SZ / tan(HALF_PI/2.0);
    g.perspective(HALF_PI, 1.0/(SZ / SEGMENTS), cameraZ/10.0, cameraZ*200.0); 
    g.noStroke();
    g.background(255);
    makeHero(g);
    g.endDraw();
    return g;    
  }
  
  void doMovement()
  {
    int n = SZ / SEGMENTS;
    viewportAspect = 1 / (((float)artEffectCounter/ART_EFFECT_FRAMES) * (n-1) + 1);
  }
  
  void makeGeometry(PGraphics g)
  {
    g.translate(SZ * teleportX, 0, SZ * teleportY);
    g.rotateY(-teleportDir.getAngle());
    makeHero(g);
  }

  void makeHero(PGraphics g)
  {    
      g.pushMatrix();
    
      g.translate(-HALF_SZ, HALF_SZ, -HALF_SZ);
                             
      g.fill(0);
      for(int n = (SEGMENTS/2)-1; n >= 0; n--)
      { 
        //This draws the segments onto the floor, which will occupy the bottom half of the 2D image.
        float d1 = (HALF_SZ / (HALF_SZ * segmentProportionsBottom[n+1])) * HALF_SZ - HALF_SZ;
        float d2 = (HALF_SZ / (HALF_SZ * segmentProportionsBottom[n])) * HALF_SZ - HALF_SZ;
        float dd1 = HALF_SZ - (d1 + HALF_SZ) / (SZ/(float)SEGMENTS);
        
        g.beginShape();
        g.vertex(dd1, 0,-d1);
        g.vertex(HALF_SZ, 0,-d2);
        g.vertex(SZ - dd1,0,-d1);
        g.endShape(CLOSE);
        
        //Now draw the segments onto the ceiling, which will occupy the top half of the 2D image.
        d1 = (HALF_SZ / (HALF_SZ * segmentProportionsTop[n+1])) * HALF_SZ - HALF_SZ;
        d2 = (HALF_SZ / (HALF_SZ * segmentProportionsTop[n])) * HALF_SZ - HALF_SZ;
        dd1 = HALF_SZ - (d1 + HALF_SZ) / (SZ/(float)SEGMENTS);      
        
        g.beginShape();
        g.vertex(dd1, -SZ,-d1);
        g.vertex(HALF_SZ, -SZ,-d2);
        g.vertex(SZ - dd1,-SZ,-d1);
        g.endShape(CLOSE);
      }
      
      g.popMatrix();
  }
}
