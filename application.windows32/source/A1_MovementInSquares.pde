class ArtMovementInSquares extends Artwork
{
  public ArtMovementInSquares(int x, int y, Direction dir)
  {
    super(x, y, dir);
    flatImage = createMovementInSquaresImage();
    teleportX = 3;
    teleportY = 2;
    teleportDir = Direction.NORTH;
  }

  PImage createMovementInSquaresImage()
  {
    PGraphics g = createGraphics(SZ,SZ, P3D);
    g.beginDraw();
    g.camera(0,0,0,0,0,-HALF_SZ,0,1,0);
    g.noStroke();
    g.background(255);
    float f = 0.001 * HALF_PI;
    float cameraZ = (height/2.0) / tan(f/2.0);
    g.perspective(f, 1.0, cameraZ/10.0, cameraZ*200.0);
    float modZ = -(HALF_SZ / tan(f / 2) - HALF_SZ);
    g.translate(0, 0, modZ);
    makeTwoCylinders(g);
    g.endDraw();
    return g;
  }
  
  void doMovement()
  {
    //We gradually increase the field of view as the animation proceeds.
    fieldOfView = ((float)(ART_EFFECT_FRAMES - artEffectCounter) / ART_EFFECT_FRAMES) * (PI / 2);
    
    //The tan function is used to calculate how far back to move the camera to compensate for the narrowed field of view.
    forwardPositionModifier = (HALF_SZ / tan(fieldOfView / 2) - HALF_SZ);
  }
  
  void makeGeometry(PGraphics g)
  {
    g.translate(SZ * teleportX, 0, SZ * teleportY);
    g.rotateY(-teleportDir.getAngle());
    makeTwoCylinders(g);
    animateCounter++;  
  }
  
  void makeTwoCylinders(PGraphics g)
  {
    float rotateAmount = -animateCounter / 100;
    
    g.pushMatrix();
    g.translate(-HALF_SZ,0,-SZ);
    g.rotateY(-rotateAmount);
    makeCylinder(g);
    g.rotateY(rotateAmount);
    g.translate(SZ, 0, 0);
    g.rotateY(rotateAmount);
    makeCylinder(g);
    g.rotateY(-rotateAmount);
    g.popMatrix();
  }
  
  void makeCylinder(PGraphics g)
  {
    //A cylinder consists of rectangular segments.
    
    float SEGMENTS = 32; //How many segments there are around the cylinder
    float SEG_ANGLE = TWO_PI / SEGMENTS;
    float VSEGMENTS = 12; //How many segments there are vertically from floor to ceiling.
    float VSEG_DISTANCE = SZ / VSEGMENTS;
    
    for(int n = 0; n < SEGMENTS; n++)
    {
      float x1 = cos(n * SEG_ANGLE) * HALF_SZ;
      float z1 = sin(n * SEG_ANGLE) * HALF_SZ;
      float x2 = cos((n+1) * SEG_ANGLE) * HALF_SZ;
      float z2 = sin((n+1) * SEG_ANGLE) * HALF_SZ;
      
      for (int y = 0; y < VSEGMENTS; y++)
      {
        g.fill(((y + n) % 2) * 255); //Rectangles alternate between black and white.
        g.beginShape();
        float yfrom = -HALF_SZ + y * VSEG_DISTANCE;
        float yto = yfrom + VSEG_DISTANCE;
        g.vertex(x1, yfrom, z1);
        g.vertex(x2, yfrom, z2);
        g.vertex(x2, yto, z2);
        g.vertex(x1, yto, z1);
        g.endShape();
      }
    }
  }
}
