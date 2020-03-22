public class Main {

    public static void main(String[] args) {
        createOriginalInstance();
//        createWorkerInstance();
    }

    public static void createOriginalInstance () {
        String ip = new OriginalEC2Launcher().launch(Configs.ORIGINAL_INSTANCE_NAME);
        System.out.println("Original instance launched. IP: " + ip);
    }

    public static void createWorkerInstance () {
        String ip = new EC2Cloner().clone(Configs.WORKER_INSTANCE_NAME);
        System.out.println("Original instance launched. IP: " + ip);
    }

}
