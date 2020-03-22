public class Main {

    public static void main(String[] args) {
//        createOriginalInstance();
//        createWorkerInstance();
    }

    public static void createOriginalInstance () {
        new OriginalEC2Launcher().launch(Configs.ORIGINAL_INSTANCE_NAME);
    }

    public static void createWorkerInstance () {
        new EC2Cloner().clone(Configs.WORKER_INSTANCE_NAME);
    }

}
