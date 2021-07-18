class ArtIntake extends Artwork
{
  float tiltAmount = -1.1f;
  float rollAmount = -0.2f;
  float panAmount = 0.49f;
  
  public ArtIntake(int x, int y, Direction dir)
  {
    super(x, y, dir);
    flatImage = createIntakeImage();
    teleportX = 2;
    teleportY = 4;
    teleportDir = Direction.EAST;
  }

  PImage createIntakeImage()
  { 
    PGraphics g = createGraphics(SZ,SZ, P3D);
    g.beginDraw();
    g.camera(0,0,0,0,0,-HALF_SZ,0,1,0);
    setStandardPerspective(g);
    g.rotateX(tiltAmount); 
    g.rotateZ(rollAmount); 
    g.rotateY(panAmount); 
    g.translate(-SZ, 0, 0);
    g.noStroke();
    g.textureMode(NORMAL);
    makeRoom(g, 3, 4, textureWaves);
    g.endDraw();
    return g;
  }
  
  void doMovement()
  {
    cameraTilt = (artEffectCounter / (float)ART_EFFECT_FRAMES) * tiltAmount; 
    cameraRoll = (artEffectCounter / (float)ART_EFFECT_FRAMES) * rollAmount;
    cameraPan = (artEffectCounter / (float)ART_EFFECT_FRAMES) * panAmount;
  }
  
  void makeGeometry(PGraphics g)
  {
    //Making the geometry for 'Intake' is handled in the main 'makeGallery' routine
  }
  
  void makeRoom(PGraphics g, int tiles_to_the_right, int tiles_ahead, PImage floor)
  {
    g.pushMatrix();
    
    for(int m = 0; m < tiles_to_the_right; m++)
    {
      g.pushMatrix();
      
      for(int n = 0; n < tiles_ahead; n++)
      {
        g.fill(0);
        g.beginShape();
        if (floor != null) g.texture(floor);
        g.vertex(-HALF_SZ, HALF_SZ, -HALF_SZ, 0, 0);
        g.vertex(HALF_SZ, HALF_SZ, -HALF_SZ, 1, 0);
        g.vertex(HALF_SZ, HALF_SZ, HALF_SZ, 1, 1);
        g.vertex(-HALF_SZ, HALF_SZ, HALF_SZ, 0, 1);
        g.endShape(CLOSE);
        g.translate(0,0,-SZ);
      }
      g.popMatrix();
      g.translate(SZ,0,0);
    }
    
    g.popMatrix();
  }
}
