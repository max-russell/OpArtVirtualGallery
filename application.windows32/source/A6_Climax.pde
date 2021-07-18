class ArtClimax extends Artwork
{  
  int SEGMENTS = 64;
  float SEG_ANGLE = PI / SEGMENTS;
  int VSEGMENTS = 10;
  float VSEG_DISTANCE = SZ / VSEGMENTS;
  
  float[][] curve_x = new float[SEGMENTS+4][2];
  float[][] curve_z = new float[SEGMENTS+4][2]; 
  
  public ArtClimax(int x, int y, Direction dir)
  {
    super(x, y, dir);
    
    //Rather than calculating the vertex positions in the half-cylinder on the fly every frame, they are pre-calculated during setup.
    for(int n = 0; n < SEGMENTS+4; n++)
    {
      float q = (cos(((float)n/SEGMENTS) * PI + PI) + 1) / 2 * PI;
      curve_z[n][0] = -sin(q) * (HALF_SZ * 3);
      curve_x[n][0] = cos(q) * (HALF_SZ * 3);
      curve_z[n][1] = -sin(q) * (HALF_SZ * 2.9);
      curve_x[n][1] = cos(q) * (HALF_SZ * 2.9);    
    }
    
    flatImage = createClimaxImage();
    teleportX = 8;
    teleportY = 11;
    teleportDir = Direction.NORTH;
  }
  
  PImage createClimaxImage()
  {
    PGraphics g = createGraphics(SZ,SZ, P3D);
    g.beginDraw();
    g.camera(0,0,0,0,0,-HALF_SZ,0,1,0);
    float cameraZ = HALF_SZ / tan(HALF_PI/2.0);
    g.perspective(HALF_PI, 0.4, cameraZ/10.0, cameraZ*200.0);  
    g.rotateZ(HALF_PI);
    g.translate(0,0,-HALF_SZ); 
    g.noStroke();
    g.background(0);
    g.textureMode(NORMAL);
    makeClimaxCurve(g);
    g.endDraw();
    return g;
  }

  void makeGeometry(PGraphics g)
  {
    g.translate(SZ * teleportX, 0, SZ * teleportY);
    g.rotateY(-teleportDir.getAngle());
    g.translate(0,0,-HALF_SZ); 
    makeClimaxCurve(g);
  }
  
  void makeClimaxCurve(PGraphics g)
  {    
    for(int n = 0; n < SEGMENTS-1; n+=4)
    { 
      for (int y = 0; y < VSEGMENTS; y++)
      {
        float yfrom = -HALF_SZ + y * VSEG_DISTANCE;
        float yto = yfrom + VSEG_DISTANCE;
        
        g.beginShape();
        g.texture(textureStripes);
        if (y % 2 == 0) //The segments protrude alternately inward and outward from top to bottom.
        {                        
          g.vertex(curve_x[n][0], yfrom, curve_z[n][0],0,0);
          g.vertex(curve_x[n+4][0], yfrom, curve_z[n+4][0],1,0);
          g.vertex(curve_x[n+5][1], yto, curve_z[n+5][1],1,1); 
          g.vertex(curve_x[n+1][1], yto, curve_z[n+1][1],0,1);
        }
        else
        {
          g.vertex(curve_x[n+1][1], yfrom, curve_z[n+1][1],0,0);
          g.vertex(curve_x[n+5][1], yfrom, curve_z[n+5][1],1,0);
          g.vertex(curve_x[n+4][0], yto, curve_z[n+4][0],1,1); 
          g.vertex(curve_x[n][0], yto, curve_z[n][0],0,1);
        }
        g.endShape();
      }  
    }    
  }
  
  void doMovement()
  {
    viewportAspect = 1 - (artEffectCounter / (float)ART_EFFECT_FRAMES) * 0.6; 
    cameraRoll = (artEffectCounter / (float)ART_EFFECT_FRAMES) * HALF_PI;
  }
}
