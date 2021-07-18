void doStepMovement()
{
  if (pauseCounter > 0) //Handling the pause before an art effect begins
  {
    pauseCounter--;
    if (pauseCounter == 0)
    {
      artEffectCounter = ART_EFFECT_FRAMES;
      actionInProgress = true;
      sndTransform.play();
      posX = currentArtEffect.teleportX;
      posZ = currentArtEffect.teleportY;
      dir = currentArtEffect.teleportDir;
      animateCounter = 0;    
    }
    else return;
  }   
  
  if (actionInProgress)
  {
    if (destAngle > angle) //Turning right
    {
      angle += HALF_PI / TURN_FRAMES;
      if (angle >= destAngle)
      { 
        dir = dir.clockwise();
        angle = destAngle = 0;
        actionInProgress = false;
      }
    }
    else if (destAngle < angle) //Turning left
    {
      angle -= HALF_PI / TURN_FRAMES;
      if (angle <= destAngle)
      {
        dir = dir.antiClockwise();
        destAngle = angle = 0;
        actionInProgress = false;
      }
    }
    else if (stepCounter > 0) //Stepping forwards
    {
      stepCounter--;
      
      forwardPositionModifier = -(STEP_FRAMES - stepCounter) * SZ/STEP_FRAMES;
      if (stepCounter == 0)
      {
        if (dir == Direction.SOUTH) posZ++;
        if (dir == Direction.NORTH) posZ--;
        if (dir == Direction.EAST) posX++;
        if (dir == Direction.WEST) posX--;        
       
        forwardPositionModifier = 0;
        actionInProgress = false;
      }
    }
    else if (stepCounter < 0) //Stepping backwards
    {
      stepCounter++;
      forwardPositionModifier = (STEP_FRAMES + stepCounter) * SZ/STEP_FRAMES;
      if (stepCounter == 0)
      {
        if (dir == Direction.SOUTH) posZ--;
        if (dir == Direction.NORTH) posZ++;
        if (dir == Direction.EAST) posX--;
        if (dir == Direction.WEST) posX++;        
       
        forwardPositionModifier = 0;
        actionInProgress = false;
      }      
    }
    
    else if (currentArtEffect != null) //Updating an art effect
    { 
      artEffectCounter--;
      
      currentArtEffect.doMovement();
      
      if (artEffectCounter == 0)
      {
        actionInProgress = false;
        forwardPositionModifier = 0;
        currentArtEffect = null;
      }
    }
    
    if (actionInProgress == false) //Check for triggering an art effect
    {
      for(Artwork artwork: map.artworks)
      {
        if (posX == artwork.x && posZ == artwork.y && dir == artwork.facingDir)
        {
          pauseCounter = PAUSE_FRAMES;
          currentArtEffect = artwork;
          sndArt.play();
        }
      }   
    }
    
  }
}
