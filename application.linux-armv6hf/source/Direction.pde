enum Direction 
{
  NORTH(0, "North"), 
  EAST(HALF_PI, "East"), 
  SOUTH(PI, "South"), 
  WEST(HALF_PI * 3, "West");
  
  private float angle;
  private String name;
  
  private Direction(float angle, String name)
  {
    this.angle = angle;
    this.name = name;
  }
  
  Direction clockwise()
  {
    if (this == NORTH) return EAST;
    else if(this == EAST) return SOUTH;
    else if(this == SOUTH) return WEST;
    else return NORTH;
  }
  
  Direction antiClockwise()
  {
    if (this == NORTH) return WEST;
    else if(this == EAST) return NORTH;
    else if(this == SOUTH) return EAST;
    else return SOUTH;
  }
  
  float getAngle() {return angle;}
  String getName() {return name;}
}
