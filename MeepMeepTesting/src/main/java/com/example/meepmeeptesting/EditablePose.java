package com.example.meepmeeptesting;

import com.acmerobotics.roadrunner.Pose2d;
import com.acmerobotics.roadrunner.Vector2d;

public final class EditablePose {

    public double x, y, heading;

    public EditablePose(double x, double y, double heading) {
        this.x = x;
        this.y = y;
        this.heading = heading;
    }

    public EditablePose(Pose2d pose) {
        this(pose.position.x, pose.position.y, pose.heading.log());
    }

    public EditablePose clone() {
        return new EditablePose(x, y, heading);
    }

    public Pose2d toPose2d() {
        return new Pose2d(x, y, heading);
    }

    public Vector2d toVector2d() {
        return new Vector2d(x, y);
    }
}
