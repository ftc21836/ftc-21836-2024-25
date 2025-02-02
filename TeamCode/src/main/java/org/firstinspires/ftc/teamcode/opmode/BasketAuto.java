package org.firstinspires.ftc.teamcode.opmode;

import static org.firstinspires.ftc.teamcode.opmode.BasketAuto.State.DRIVING_TO_SUB;
import static org.firstinspires.ftc.teamcode.opmode.BasketAuto.State.INTAKING_2;
import static org.firstinspires.ftc.teamcode.opmode.BasketAuto.State.INTAKING_3;
import static org.firstinspires.ftc.teamcode.opmode.BasketAuto.State.PARKING;
import static org.firstinspires.ftc.teamcode.opmode.BasketAuto.State.PRELOAD_AND_1;
import static org.firstinspires.ftc.teamcode.opmode.BasketAuto.State.SCORING;
import static org.firstinspires.ftc.teamcode.opmode.BasketAuto.State.SCORING_1;
import static org.firstinspires.ftc.teamcode.opmode.BasketAuto.State.SCORING_2;
import static org.firstinspires.ftc.teamcode.opmode.BasketAuto.State.SWEEPING;
import static org.firstinspires.ftc.teamcode.opmode.Auto.EXTEND_SAMPLE_1;
import static org.firstinspires.ftc.teamcode.opmode.Auto.EXTEND_SAMPLE_2;
import static org.firstinspires.ftc.teamcode.opmode.Auto.EXTEND_SAMPLE_3;
import static org.firstinspires.ftc.teamcode.opmode.Auto.EXTEND_SUB_MAX;
import static org.firstinspires.ftc.teamcode.opmode.Auto.EXTEND_SUB_MIN;
import static org.firstinspires.ftc.teamcode.opmode.Auto.INCREMENT_LOWERING_BUCKET;
import static org.firstinspires.ftc.teamcode.opmode.Auto.LENGTH_START_DROPPING_BUCKET;
import static org.firstinspires.ftc.teamcode.opmode.Auto.SPEED_INTAKING;
import static org.firstinspires.ftc.teamcode.opmode.Auto.TIME_CYCLE;
import static org.firstinspires.ftc.teamcode.opmode.Auto.TIME_EXTEND_CYCLE;
import static org.firstinspires.ftc.teamcode.opmode.Auto.TIME_SCORE;
import static org.firstinspires.ftc.teamcode.opmode.Auto.WAIT_POST_INTAKING;

import static java.lang.Math.PI;
import static java.lang.Math.cos;
import static java.lang.Math.min;

import androidx.annotation.NonNull;

import com.acmerobotics.dashboard.telemetry.TelemetryPacket;
import com.acmerobotics.roadrunner.Action;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.subsystem.Robot;

import java.util.ArrayList;

class BasketAuto implements Action {

    enum State {
        PRELOAD_AND_1,
        SCORING_1,
        INTAKING_2,
        SCORING_2,
        INTAKING_3,
        SCORING,
        DRIVING_TO_SUB,
        SWEEPING,
        PARKING
    }

    private State state = PRELOAD_AND_1;

    private ElapsedTime matchTimer = null;
    private final ElapsedTime timer = new ElapsedTime(), bucketTimer = new ElapsedTime();

    private Action activeTraj;

    private final Robot robot;
    private final Action
            score1,
            intake2,
            score2,
            intake3,
            score3,
            park,
            i1To2,
            i2To3,
            i3ToSub,
            subPark;

    private final ArrayList<Action>
            toSubs,
            sweepLefts,
            sweepRights,
            scores;

    private boolean sweepingLeft = true;

    BasketAuto(
            Robot robot,
            Action preloadAnd1,
            Action score1,
            Action intake2,
            Action score2,
            Action intake3,
            Action score3,
            Action park,
            Action i1To2,
            Action i2To3,
            Action i3ToSub,
            Action subPark,
            ArrayList<Action> toSubs,
            ArrayList<Action> sweepLefts,
            ArrayList<Action> sweepRights,
            ArrayList<Action> scores
    ) {
        this.robot = robot;
        activeTraj = preloadAnd1;
        this.score1 = score1;
        this.intake2 = intake2;
        this.score2 = score2;
        this.intake3 = intake3;
        this.score3 = score3;
        this.park = park;
        this.i1To2 = i1To2;
        this.i2To3 = i2To3;
        this.i3ToSub = i3ToSub;
        this.subPark = subPark;
        this.toSubs = toSubs;
        this.sweepLefts = sweepLefts;
        this.sweepRights = sweepRights;
        this.scores = scores;
    }

    public boolean run(@NonNull TelemetryPacket p) {
        if (matchTimer == null) matchTimer = new ElapsedTime();

        double remaining = 30 - matchTimer.seconds();

        boolean hasSample = robot.intake.hasSample();

        boolean intaking = state == PRELOAD_AND_1 || state == INTAKING_2 || state == INTAKING_3 || state == SWEEPING;
        boolean stopMoving = intaking && hasSample;
        boolean trajDone = !stopMoving && !activeTraj.run(p);

        switch (state) {
            case PRELOAD_AND_1:

                if (robot.intake.extendo.atPosition(EXTEND_SAMPLE_1))
                    robot.intake.runRoller(1);

                // Sample intaked
                if (!hasSample) {

                    timer.reset();

                    // skip to 2 if didn't get 1
                    if (trajDone) {
                        activeTraj = i1To2;
                        state = INTAKING_2;
                    }

                } else {
                    robot.intake.runRoller(1);
                    if (timer.seconds() >= WAIT_POST_INTAKING) {
                        robot.intake.runRoller(0);
                        activeTraj = score1;
                        state = SCORING_1;
                    }
                }

                break;

            case SCORING_1:
                if (trajDone) {
                    activeTraj = intake2;
                    state = INTAKING_2;
                }
                break;
            case INTAKING_2:

                if (robot.intake.extendo.atPosition(EXTEND_SAMPLE_2))
                    robot.intake.runRoller(1);

                // Sample intaked
                if (!hasSample) {

                    timer.reset();

                    // skip to 3 if didn't get 2
                    if (trajDone) {
                        activeTraj = i2To3;
                        state = INTAKING_3;
                    }

                } else {
                    robot.intake.runRoller(1);
                    if (timer.seconds() >= WAIT_POST_INTAKING) {
                        robot.intake.runRoller(0);
                        activeTraj = score2;
                        state = SCORING_2;
                    }
                }

                break;

            case SCORING_2:
                if (trajDone) {
                    activeTraj = intake3;
                    state = INTAKING_3;
                }
                break;
            case INTAKING_3:

                if (robot.intake.extendo.atPosition(EXTEND_SAMPLE_3))
                    robot.intake.runRoller(1);

                // Sample intaked
                if (!hasSample) {

                    timer.reset();

                    // skip to sub if didn't get 3
                    if (trajDone) {
                        activeTraj = i3ToSub;
                        state = DRIVING_TO_SUB;
                    }

                } else {
                    robot.intake.runRoller(1);
                    if (timer.seconds() >= WAIT_POST_INTAKING) {
                        robot.intake.runRoller(0);
                        activeTraj = score3;
                        state = SCORING;
                    }
                }

                break;

            case SCORING:
                if (trajDone) {
                    if (remaining < TIME_CYCLE) {
                        activeTraj = park;
                        state = PARKING;
                    } else {
                        activeTraj = toSubs.remove(0);
                        state = DRIVING_TO_SUB;
                    }
                }
                break;

            case DRIVING_TO_SUB:
                if (trajDone) {
                    sweepingLeft = true;
                    activeTraj = sweepLefts.remove(0);
                    state = SWEEPING;
                    bucketTimer.reset();
                }
                break;
            case SWEEPING:

                if (remaining < TIME_SCORE) {
                    activeTraj = subPark;
                    state = PARKING;
                    break;
                }

                // Sample intaked
                if (!hasSample) {

                    /// <a href="https://www.desmos.com/calculator/iazwdw6hky">Graph</a>
                    robot.intake.extendo.setTarget(
                            EXTEND_SUB_MIN + (EXTEND_SUB_MAX - EXTEND_SUB_MIN) * (1 - cos(2 * PI * remaining / TIME_EXTEND_CYCLE)) / 2
                    );
                    if (robot.intake.extendo.getPosition() >= LENGTH_START_DROPPING_BUCKET)
                        robot.intake.runRoller(min(bucketTimer.seconds() * INCREMENT_LOWERING_BUCKET, SPEED_INTAKING));

                    timer.reset();

                    // sweep the other way
                    if (trajDone) {
                        sweepingLeft = !sweepingLeft;
                        activeTraj = (sweepingLeft ? sweepRights : sweepLefts).remove(0);
                    }

                } else {
                    robot.intake.runRoller(1);
                    if (timer.seconds() >= WAIT_POST_INTAKING) {
                        robot.intake.runRoller(0);
                        activeTraj = scores.remove(0);
                        state = SCORING;
                    }
                }

                break;

            case PARKING:
                return !trajDone;
        }

        return true;
    }
}
