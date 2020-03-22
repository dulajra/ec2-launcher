public class Main {

    public static void main(String[] args) {
//        createOriginalInstance();
//        createWorkerInstance();
        createAndClone();
    }

    public static void createOriginalInstance() {
        String ip = new OriginalEC2Launcher().launch(Configs.ORIGINAL_INSTANCE_NAME);
        System.out.println("Original instance launched. IP: " + ip);
    }

    public static void createWorkerInstance() {
        String ip = new WorkerEC2Launcher().launch(Configs.WORKER_INSTANCE_NAME, Configs.WORKER_INSTANCE_AMI_ID);
        System.out.println("Original instance launched. IP: " + ip);
    }

    public static void createAndClone() {
        String originalInstanceIP = new OriginalEC2Launcher().launch(Configs.ORIGINAL_INSTANCE_NAME);
        System.out.println("Original instance launched. IP: " + originalInstanceIP);
        String clonedInstanceIP = new WorkerEC2Launcher().clone(Configs.WORKER_INSTANCE_NAME, originalInstanceIP);
        System.out.println("Cloned instance launched. IP: " + originalInstanceIP);
    }

}
