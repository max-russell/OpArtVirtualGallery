import processing.sound.*;

//These constants can be adjusted for better performance. Lowering 'SZ' may make the sketch run better on low-spec machines. 
final int FRAME_RATE = 25;
final int SZ = 800; //This is the size of a unit of length in the sketch, by which all other measurements in the gallery are derived. It is also the size of the viewport.

//These control the duration of the various actions in the sketch (in seconds)
final float STEP_DURATION = 0.5; //Time taken to step forwards or backwards
final float TURN_DURATION = 0.5; //Time take to turn 90 degress
final float PAUSE_DURATION = 0.5; //Pause duration just before starting the art effect.
final float ART_EFFECT_DURATION = 2.0; //Time taken for the art effect.

//These constants are derived from the above.
final int HALF_SZ = SZ / 2;
final int STEP_FRAMES = (int)(STEP_DURATION * FRAME_RATE);
final int TURN_FRAMES = (int)(TURN_DURATION * FRAME_RATE);
final int ART_EFFECT_FRAMES = (int)(ART_EFFECT_DURATION * FRAME_RATE);
final int PAUSE_FRAMES = (int)(PAUSE_DURATION * FRAME_RATE);
final int TEXT_SIZE = max(9, SZ / 42);

//Global variables storing the user's position and orientation, frame counters, and perspective/camera settings.
Direction dir = Direction.NORTH;
float posX = 0;
float posZ = 0;
float cameraTilt = 0;
float cameraRoll = 0;
float cameraPan = 0;
float fieldOfView = HALF_PI;
float viewportAspect = 1.0;
float forwardPositionModifier = 0;
float angle = 0, destAngle = 0;
float animateCounter = 0;
int stepCounter = 0;
int artEffectCounter = 0;
int pauseCounter = 0;
Artwork currentArtEffect = null;
boolean actionInProgress = false;
boolean displayDiagnostics = false;

//The Map object in which the gallery's layout data is stored.
Map map;

//Textures used as a basis for constructing some of the art pieces
PImage[] textureCircles = new PImage[4];
PImage textureWaves;
PImage textureStripes;

SoundFile sndMove, sndInaction, sndArt, sndTransform;

void settings()
{
  size(SZ, SZ, P3D);
}

void setup()
{
  frameRate(FRAME_RATE);
  camera(0,0,0,0,0,-HALF_SZ,0,1,0);
  textureMode(NORMAL);
  noStroke();
  
  PGraphics g;
  
  //Create the textures for the animating circles used to mark the position of an artwork
  for(int n = 0; n < 4; n++)
  {
    g = createGraphics(SZ,SZ);
    g.beginDraw();
    g.noStroke();
    g.background(0);
    g.fill(255);
    g.ellipse(SZ / 2, SZ / 2, SZ/10 * (n+1), SZ/10 * (n+1));
    g.fill(0);
    g.ellipse(SZ / 2, SZ / 2, SZ/10 * n, SZ/10 * n);
    g.endDraw();
    textureCircles[n] = g;
  }
  
  //Create the 'Stripes' texture image. This is used in 'Opening' and 'Climax'.
  g = createGraphics(SZ,SZ);
  g.beginDraw();
  g.noStroke();
  g.background(255);
  g.fill(0);
  for(int n = 0; n < 16; n++)
  {
    g.rect(n * SZ / 16, 0, SZ/32, SZ);
  }
  g.endDraw();
  textureStripes = g;
  
  //Create the 'Waves' texture image. This is used in 'Intake' and 'Fall'.
  g = createGraphics(SZ,SZ, P3D);
  g.beginDraw();
  for(int n = -SZ/8; n <= SZ + SZ/8; n += SZ/4)
  {
    drawWave(g, n, SZ/8);
  }
  g.endDraw();
  textureWaves = g;
  
  //Initalise the map object, which stores the layout of the gallery.
  map = new Map();
  posX = map.startX;
  posZ = map.startY;  
  
  sndMove = new SoundFile(this, "movement.wav");
  sndInaction = new SoundFile(this, "no_action.wav");
  sndArt = new SoundFile(this, "FX01.aif");
  sndTransform = new SoundFile(this, "sfx_sounds_interaction8.wav");
}

void draw()
{ 
  background(255);
  
  doStepMovement();
  
  pushMatrix();
  
  float cameraZ = (height/2.0) / tan(fieldOfView/2.0);
  perspective(fieldOfView, viewportAspect, cameraZ/10.0, cameraZ*200.0);
  
  //Matrix transformations based on the user's position and orientation.
  rotateX(cameraTilt);
  rotateZ(cameraRoll);
  rotateY(cameraPan);
  translate(0,0,-forwardPositionModifier);
  rotateY(dir.getAngle() + angle);
  translate(-(posX * SZ), 0, -(posZ * SZ));
  
  if (currentArtEffect instanceof ArtMovementInSquares && pauseCounter == 0)
  {
    //This is a fix for 'Movement in Squares', to hide other geometry while the art effect is running.
    //The rest of the gallery gets in the way otherwise, due to the extreme camera movement. 
    currentArtEffect.makeGeometry(g);
  }
  else
  { 
    //Make the walls, floors and ceilings of the gallery
    makeGallery(g);
 
    //Make each art piece, consisting of the wall image, and its corresponding geometry in the gallery.
    for(Artwork aw: map.artworks)
    {
      g.pushMatrix();
      g.translate(SZ * aw.x, 0, SZ * aw.y);
      rotateY(-aw.facingDir.getAngle());
      makeWall(g, aw.flatImage);
      g.popMatrix();
      
      g.pushMatrix();
      aw.makeGeometry(g);
      g.popMatrix();
    }
  }
  
  popMatrix();
  
  if (displayDiagnostics)
  {
    drawDiagnostics();
  }
} 

void makeGallery(PGraphics g)
{
  //Reads the layout data in the map object and constructs the basic walls, floors and ceilings of the gallery.
  
  g.pushMatrix();
  for(int y = 0; y < map.getHeight(); y++)
  {
    g.pushMatrix();
    for(int x = 0; x < map.getWidth(); x++)
    {
      if (map.floorAt(x,y) > 0)
      {
        g.fill(0);
        //Floor
        g.beginShape();
        if (map.floorAt(x,y) == 2) 
        {
          g.texture(textureWaves);
        }
        else if (map.floorAt(x,y) == 3) 
        {
          g.texture(textureCircles[(int)(animateCounter / (FRAME_RATE * 0.3)) % 4]);
        }
        
        g.vertex(-HALF_SZ, HALF_SZ, -HALF_SZ, 0, 1);
        g.vertex(HALF_SZ, HALF_SZ, -HALF_SZ, 0, 0);
        g.vertex(HALF_SZ, HALF_SZ, HALF_SZ, 1, 0);
        g.vertex(-HALF_SZ, HALF_SZ, HALF_SZ, 1, 1);
        g.endShape(CLOSE);
        
        //Ceiling
        g.beginShape();
        g.vertex(-HALF_SZ, -HALF_SZ, -HALF_SZ,0,0);
        g.vertex(HALF_SZ, -HALF_SZ, -HALF_SZ,1,0);
        g.vertex(HALF_SZ, -HALF_SZ, HALF_SZ,1,1);
        g.vertex(-HALF_SZ, -HALF_SZ, HALF_SZ,0,1);
        g.endShape(CLOSE);
      }
      
      //Walls in each cardinal direction.
      Direction w = Direction.NORTH;
      for(int d = 0; d < 4; d++)
      {
        if (map.wallAt(x,y,w))
        {
          makeWall(g, null);
        }
        rotateY(-HALF_PI);
        w = w.clockwise();
      }
      
      g.translate(SZ,0,0);
    }
    g.popMatrix();
    g.translate(0,0,SZ);
  }
  g.popMatrix();
  
  g.pushMatrix();
  g.translate(map.startX * SZ, 0, map.startY * SZ);
  makeInstructions(g);
  g.popMatrix(); 
}

void makeWall(PGraphics g, PImage wall)
{
  g.translate(0,0, -HALF_SZ);
  fill(255);
  if (wall == null)
  {
    rect(-HALF_SZ, -HALF_SZ, SZ, SZ);
  }
  else
  {
    g.image(wall, -HALF_SZ, -HALF_SZ);
  }
  g.translate(0,0, HALF_SZ);
}

public void makeInstructions(PGraphics g)
{
  //The instructions are on the north wall at the start position, and the first thing the player sees when the sketch begins.
  g.pushMatrix();
  
  //First construct the 'plaque' on which the instruction text is written.
  int w1 = (int)(SZ / 2.5f);
  int h1 = SZ / 3;
  int w2 = (int)(w1 - SZ/16);
  int h2 = (int)(h1 - SZ/16);
  int d = SZ / 24;
  
  g.translate(0,0, -HALF_SZ);
  g.beginShape();
  g.texture(textureStripes);
  g.vertex(-w1, -h1, 0, 0.25f, 0.25f);
  g.vertex(w1, -h1, 0, 0.25f, 0);
  g.vertex(w2, -h2, d, 0, 0.25f);
  g.vertex(-w2, -h2, d, 0, 0);
  g.endShape();
  
  g.beginShape();
  g.texture(textureStripes);
  g.vertex(-w2, h2, d, 0, 0);
  g.vertex(w2, h2, d, 0, 0.25f);
  g.vertex(w1, h1, 0, 0.25f, 0);
  g.vertex(-w1, h1, 0, 0.25f, 0.25f);
  g.endShape();  
  
  g.beginShape();
  g.texture(textureStripes);
  g.vertex(-w1, h1, 0, 0.25f, 0.25f);
  g.vertex(-w1, -h1, 0, 0.25f, 0);
  g.vertex(-w2, -h2, d, 0, 0.25f);
  g.vertex(-w2, h2, d, 0, 0);
  g.endShape();    
  
  g.beginShape();
  g.texture(textureStripes);
  g.vertex(w2, h2, d, 0, 0);
  g.vertex(w2, -h2, d, 0, 0.25f);
  g.vertex(w1, -h1, 0, 0.25f, 0);
  g.vertex(w1, h1, 0, 0.25f, 0.25f);
  g.endShape();  
  
  g.beginShape();
  g.vertex(-w2, -h2, d, 0, 0);
  g.vertex(w2, -h2, d, 1, 0);
  g.vertex(w2, h2, d, 1, 1);
  g.vertex(-w2, h2, d, 0, 1);
  g.endShape();
  
  //Now write the text itself onto the plaque.
  
  String introMessage =
  "WELCOME TO THE\n" +
  "BRIDGET RILEY VIRTUAL GALLERY\n\n" +
  "Use the arrow keys to turn left or right\n" +
  "and to step forwards or backwards. Or\n" +
  "alternatively, click the edges of the screen.\n\n" +
  "Step on the circles on the floor and face\n" +
  "the wall to see an artistic transformation.\n\n" +
  "Now turn LEFT to begin.";
  
  g.translate(0,0,d + SZ/128);
  g.fill(0);
  g.textSize(TEXT_SIZE);
  g.textAlign(CENTER);
  g.text(introMessage,0, - TEXT_SIZE * 7);
  g.translate(0,0, HALF_SZ);
  g.popMatrix();
}

void drawWave(PGraphics g, int x, int thickness)
{ 
  g.noStroke();
  g.fill(0);
  g.beginShape();
  g.vertex(x, 0);
  g.bezierVertex(x - HALF_SZ, HALF_SZ, x+HALF_SZ, HALF_SZ, x, SZ);
  g.vertex(x, SZ);
  g.vertex(x + thickness, SZ);
  g.bezierVertex(x + HALF_SZ + thickness, HALF_SZ, x - SZ/2+thickness, HALF_SZ, x+thickness, 0);
  g.vertex(x+thickness, 0);
  g.endShape();
}

void setStandardPerspective(PGraphics g)
{
  float cameraZ = HALF_SZ / tan(HALF_PI/2.0);
  g.perspective(HALF_PI, 1.0, cameraZ/10.0, cameraZ*200.0);  
}

void drawDiagnostics()
{
  setStandardPerspective(g);
  
  translate(0,0, -HALF_SZ);
  fill(255,0,0);
  textAlign(LEFT);
  
  textSize(TEXT_SIZE);
  text((int)posX + ", " + (int)posZ + " (" + dir.getName() + ")", 0, -HALF_SZ + TEXT_SIZE);
  text("Tilt: " + cameraTilt + "   Pan: " + cameraPan + "   Roll: " + cameraRoll, 0, -HALF_SZ + 2 * TEXT_SIZE);
  text("Aspect: " + viewportAspect, 0, -HALF_SZ + 3 * TEXT_SIZE);  
  text("fov: " + fieldOfView, 0, -HALF_SZ + 4 * TEXT_SIZE);
  text("forward: " + forwardPositionModifier, 0, -HALF_SZ + 5 * TEXT_SIZE);
}
