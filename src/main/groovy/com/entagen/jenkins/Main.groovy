package com.entagen.jenkins


/*
Bootstrap class that parses command line arguments, or system properties passed in by jenkins, and starts the jenkins-build-per-branch sync process
 */
class Main {
    public static final Map<String, Map<String, Object>> opts = [
            h: [longOpt: 'help', required: false, args: 0, argName: 'help', description: "Print usage information - gradle flag -Dhelp=true"],
            j: [longOpt: 'jenkins-url', required: true, args: 1, argName: 'jenkinsUrl', description: "Jenkins URL - gradle flag -DjenkinsUrl=<jenkinsUrl>"],
            u: [longOpt: 'git-url',  required: true, args: 1, argName: 'gitUrl', description: "Git Repository URL - gradle flag -DgitUrl=<gitUrl>"],
            p: [longOpt: 'job-prefix', required: true, args: 1, argName: 'templateJobPrefix', description: "Template Job Prefix, - gradle flag -DtemplateJobPrefix=<jobPrefix>"],
            t: [longOpt: 'template-branch', required: true, args: 1, argName:  'templateBranchName', description: "Template Branch Name - gradle flag -DtemplateBranchName=<branchName>"],
            n: [longOpt: 'nested-view', required: false, args: 1, argName: 'nestedView', description: "Nested Parent View Name - gradle flag -DnestedView=<nestedView> - optional - must have Jenkins Nested View Plugin installed"],
            c: [longOpt: 'print-config', required: false, args: 0, argName: 'printConfig', description:  "Check configuration - print out settings then exit - gradle flag -DprintConfig=true"],
            d: [longOpt: 'dry-run', required: false, args: 0, argName: 'dryRun', description:  "Dry run, don't actually modify, create, or delete any jobs, just print out what would happen - gradle flag: -DdryRun=true"]
    ]

    public static void main(String[] args) {
        Map<String, String> argsMap = parseArgs(args)
        showConfiguration(argsMap)
        JenkinsJobManager manager = new JenkinsJobManager(argsMap)
        manager.syncWithRepo()
    }

    public static Map<String, String> parseArgs(String[] args) {
        def cli = createCliBuilder()
        OptionAccessor commandLineOptions = cli.parse(args)

        // this is necessary as Gradle's command line parsing stinks, it only allows you to pass in system properties (or task properties which are basically the same thing)
        // we need to merge in those properties in case the script is being called from `gradle syncWithGit` and the user is giving us system properties
        Map<String, String> argsMap = mergeSystemPropertyOptions(commandLineOptions)

        if (argsMap.help) {
            cli.usage()
            System.exit(0)
        }

        if (argsMap.printConfig) {
            showConfiguration(argsMap)
            System.exit(0)
        }

        def missingArgs = opts.findAll { shortOpt, optMap ->
            if (optMap.required) return !argsMap."${optMap.argName}"
        }

        if(missingArgs) {
            missingArgs.each {shortOpt, missingArg -> println "missing required argument: ${missingArg.argName}"}
            cli.usage()
            System.exit(1)
        }

        return argsMap
    }

    public static createCliBuilder() {
        def cli = new CliBuilder(usage: "jenkins-build-per-branch [options]", header: 'Options, if calling from `gradle syncWithGit`, you need to use a system property format -D<argName>=value, ex: (gradle -DgitUrl=git@github.com:yourname/yourrepo.git syncWithGit):')
        opts.each { String shortOpt, Map<String, Object> optMap ->
            if (optMap.args) {
                cli."$shortOpt"(longOpt: optMap.longOpt, args: optMap.args, argName: optMap.argName, optMap.description)
            } else {
                cli."$shortOpt"(longOpt: optMap.longOpt, optMap.description)
            }
        }
        return cli
    }

    public static showConfiguration(Map<String, String> argsMap) {
        println "==============================================================="
        argsMap.each { k, v -> println " $k: $v" }
        println "==============================================================="
    }

    public static Map<String, String> mergeSystemPropertyOptions(OptionAccessor commandLineOptions) {
        Map <String, String> mergedArgs = [:]
        opts.each { String shortOpt, Map<String, String> optMap ->
            if (optMap.argName) {
                mergedArgs[optMap.argName] = commandLineOptions."$shortOpt" ?: System.getProperty(optMap.argName)
            }
        }
        return mergedArgs.findAll { k, v -> v }
    }
}