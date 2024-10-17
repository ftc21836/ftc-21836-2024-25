package org.firstinspires.ftc.teamcode.subsystems;

import static com.arcrobotics.ftclib.hardware.motors.Motor.GoBILDA.RPM_1620;
import static com.arcrobotics.ftclib.hardware.motors.Motor.ZeroPowerBehavior.FLOAT;
import static org.firstinspires.ftc.teamcode.opmodes.AutonVars.isRed;
import static org.firstinspires.ftc.teamcode.opmodes.MainAuton.mTelemetry;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.Sample.BLUE;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.Sample.NONE;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.Sample.RED;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.State.BUCKET_PIVOTING;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.State.BUCKET_RAISING;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.State.BUCKET_RETRACTING;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.State.DROPPING_BAD_SAMPLE;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.State.EXTENDO_RETRACTING;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.State.INTAKING;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.State.RETRACTED;
import static org.firstinspires.ftc.teamcode.subsystems.Intake.State.WAITING_FOR_DEPOSIT;
import static org.firstinspires.ftc.teamcode.subsystems.utilities.SimpleServoPivot.getAxonServo;
import static org.firstinspires.ftc.teamcode.subsystems.utilities.SimpleServoPivot.getGoBildaServo;
import static org.firstinspires.ftc.teamcode.subsystems.utilities.SimpleServoPivot.getReversedServo;
import static java.lang.Math.acos;
import static java.lang.Math.pow;

import com.acmerobotics.dashboard.config.Config;
import com.arcrobotics.ftclib.hardware.motors.MotorEx;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.TouchSensor;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.control.gainmatrices.HSV;
import org.firstinspires.ftc.teamcode.subsystems.utilities.SimpleServoPivot;
import org.firstinspires.ftc.teamcode.subsystems.utilities.sensors.ColorSensor;

@Config
public final class Intake {

    public static double
            ANGLE_BUCKET_RETRACTED = 9,
            ANGLE_BUCKET_INTAKING = 196.5,
            ANGLE_BUCKET_FLOOR_CLEARANCE = 60,
            ANGLE_BUCKET_VERTICAL = 90,
            ANGLE_LATCH_TRANSFERRING = 0,
            ANGLE_LATCH_INTAKING = 105,
            ANGLE_LATCH_LOCKED = 159,
            ANGLE_EXTENDO_RETRACTED = 0,
            ANGLE_EXTENDO_EXTENDED = 150,
            TIME_BUCKET_PIVOT = 1,
            TIME_DROP = 1,
            TIME_BUCKET_RAISE_TO_EXTEND = 1,
            TIME_BUCKET_RAISE_TO_DEPOSIT_LIFTING = 1,
            TIME_REVERSING = 0.175,
            SPEED_REVERSING = -0.6,
            COLOR_SENSOR_GAIN = 1,
            DISTANCE_EXTENDO_RETRACTED = 67.4,
            DISTANCE_EXTENDO_LINKAGE_ARM = pow(240, 2);

    /**
     * HSV value bound for intake pixel detection
     */
    public static HSV
            minRed = new HSV(
                    205,
                    0.55,
                    0.01
            ),
            maxRed = new HSV(
                    225,
                    1,
                    0.35
            ),
            minYellow = new HSV(
                    90,
                    0.4,
                    0.01
            ),
            maxYellow = new HSV(
                    125,
                    1,
                    0.15
            ),
            minBlue = new HSV(
                    130,
                    0.5,
                    0.01
            ),
            maxBlue = new HSV(
                    160,
                    1,
                    0.2
            );

    public enum Sample {
        NONE,
        NEUTRAL,
        BLUE,
        RED;

        /**
         * @return The {@link Sample} corresponding to the provided {@link HSV} as per the tuned value bounds
         */
        public static Sample fromHSV(HSV hsv) {
            return
                    hsv.between(minRed, maxRed) ? RED :
                    hsv.between(minBlue, maxBlue) ? BLUE :
                    hsv.between(minYellow, maxYellow) ? NEUTRAL :
                    NONE;
        }
    }

    private final MotorEx motor;

    private final ColorSensor colorSensor;
    private HSV hsv = new HSV();
    Sample sample = NONE;

    private final TouchSensor bucketSensor, extendoSensor;

    private final SimpleServoPivot bucket, latch, extendo;

    private Intake.State state = RETRACTED;

    private final ElapsedTime timer = new ElapsedTime(), timeSinceBucketRetracted = new ElapsedTime();

    private boolean isIntaking = false;
    private double motorPower;

    enum State {
        RETRACTED,
        BUCKET_RAISING,
        INTAKING,
        BUCKET_PIVOTING,
        DROPPING_BAD_SAMPLE,
        EXTENDO_RETRACTING,
        WAITING_FOR_DEPOSIT,
        BUCKET_RETRACTING,
    }

    Intake(HardwareMap hardwareMap) {

        bucket = new SimpleServoPivot(
                ANGLE_BUCKET_RETRACTED,
                ANGLE_BUCKET_VERTICAL,
                getAxonServo(hardwareMap, "intake right"),
                getReversedServo(getAxonServo(hardwareMap, "intake left"))
        );

        latch = new SimpleServoPivot(
                ANGLE_LATCH_TRANSFERRING,
                ANGLE_LATCH_LOCKED,
                getGoBildaServo(hardwareMap, "latch right"),
                getReversedServo(getGoBildaServo(hardwareMap, "latch left"))
        );

        extendo = new SimpleServoPivot(
                ANGLE_EXTENDO_RETRACTED,
                ANGLE_EXTENDO_EXTENDED,
                getGoBildaServo(hardwareMap, "extendo right"),
                getReversedServo(getGoBildaServo(hardwareMap, "extendo left"))
        );

        motor = new MotorEx(hardwareMap, "intake", RPM_1620);
        motor.setZeroPowerBehavior(FLOAT);
        motor.setInverted(true);

        colorSensor = new ColorSensor(hardwareMap, "bucket color", (float) COLOR_SENSOR_GAIN);

        bucketSensor = hardwareMap.get(TouchSensor.class, "bucket pivot sensor");
        extendoSensor = hardwareMap.get(TouchSensor.class, "extendo sensor");

        timer.reset();
    }

    void run(boolean depositHasSample, boolean depositIsExtended, boolean depositTryingToExtend) {

        boolean depositIsActive = depositTryingToExtend || depositIsExtended;

        switch (state) {

            case RETRACTED:

                if (isIntaking) {
                    bucket.setActivated(true);
                    state = BUCKET_RAISING;
                    timer.reset();
                } else {
                    bucket.setActivated(depositIsActive);
                    break;
                }

            case BUCKET_RAISING:

                if (timer.seconds() >= TIME_BUCKET_RAISE_TO_EXTEND) {
                    extendo.setActivated(true);
                    state = INTAKING;
                } else break;

            case INTAKING:

                Sample badSample = isRed ? BLUE : RED;

                colorSensor.update();
                hsv = colorSensor.getHSV();
                Sample sample = Sample.fromHSV(hsv);

                if (sample == badSample) {
                    latch.setActivated(true);
                    state = BUCKET_PIVOTING;
                    timer.reset();
                } else if (sample != NONE || !isIntaking) {
                    if (sample != NONE) latch.setActivated(true);
                    this.sample = sample;
                    state = EXTENDO_RETRACTING;
                    extendo.setActivated(false);
                    timer.reset();
                    break;
                } else break;

            case BUCKET_PIVOTING:

                if (timer.seconds() >= TIME_BUCKET_PIVOT) {
                    latch.setActivated(false);
                    state = DROPPING_BAD_SAMPLE;
                    timer.reset();
                } else break;

            case DROPPING_BAD_SAMPLE:

                if (timer.seconds() >= TIME_DROP) {
                    state = INTAKING;
                } else break;

            case EXTENDO_RETRACTING:

                if (extendoSensor.isPressed()) state = WAITING_FOR_DEPOSIT;
                else if (timer.seconds() <= TIME_REVERSING) setMotorPower(SPEED_REVERSING);
                else break;

            case WAITING_FOR_DEPOSIT:

                if (this.sample == NONE) {
                    state = RETRACTED;
                    isIntaking = false;
                } else if (!depositIsExtended && !depositHasSample) {
                    bucket.setActivated(false);
                    state = BUCKET_RETRACTING;
                } else break;

            case BUCKET_RETRACTING:

                if (bucketSensor.isPressed()) {
                    state = RETRACTED;
                    isIntaking = false;
                } else break;
        }

        if (!bucket.isActivated()) timeSinceBucketRetracted.reset();

        if (state != INTAKING) setMotorPower(0);

        double ANGLE_BUCKET_DOWN =
                state == INTAKING ? ANGLE_BUCKET_INTAKING - (motorPower != 0 ? 0 : ANGLE_BUCKET_FLOOR_CLEARANCE) :
                ANGLE_BUCKET_VERTICAL;

        bucket.updateAngles(ANGLE_BUCKET_RETRACTED,ANGLE_BUCKET_DOWN);

        double ANGLE_LATCH_UNLOCKED = (state == INTAKING || state == DROPPING_BAD_SAMPLE) ? ANGLE_LATCH_INTAKING : ANGLE_LATCH_TRANSFERRING;

        latch.updateAngles(ANGLE_LATCH_UNLOCKED, ANGLE_LATCH_LOCKED);

        bucket.run();
        latch.run();
        extendo.run();

        motor.set(motorPower);
    }

    boolean awaitingTransfer() {
        return state == RETRACTED && sample != NONE;
    }

    void releaseSample() {
        latch.setActivated(false);
    }

    boolean clearOfDeposit() {
        return timeSinceBucketRetracted.seconds() >= TIME_BUCKET_RAISE_TO_DEPOSIT_LIFTING;
    }

    public void setMotorPower(double motorPower) {
        if (motorPower != 0) isIntaking = true;
        this.motorPower = motorPower;
    }

    public void setExtended(boolean isIntaking) {
        this.isIntaking = isIntaking;
    }

    public void toggle() {
        setExtended(!isIntaking);
    }

    public static double extensionToAngle(double millimeters) {
        return 0.5 * acos(1 - pow(millimeters + DISTANCE_EXTENDO_RETRACTED, 2) / (2 * DISTANCE_EXTENDO_LINKAGE_ARM));
    }

    void printTelemetry() {
        mTelemetry.addData("Bucket contains", (sample == NONE ? "no" : "a " + sample.name()) + " sample");
    }

    void printNumericalTelemetry() {
        hsv.toTelemetry("Bucket HSV");
    } 
}
