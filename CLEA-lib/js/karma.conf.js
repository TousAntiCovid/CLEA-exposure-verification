// Karma configuration
// Generated on Wed Mar 24 2021 19:19:48 GMT+0100 (heure normale d’Europe centrale)

module.exports = function(config) {
  config.set({

    // log result for final check with java
    "logReporter": {
      'outputPath': '',
      "outputName": "crypto.csv",
      "filter_key": "crypto-filter"
    },

    // base path that will be used to resolve all patterns (eg. files, exclude)
    basePath: '',

    // frameworks to use
    // available frameworks: https://npmjs.org/browse/keyword/karma-adapter
    frameworks: ['mocha', 'chai'],

    // plugins used
    plugins: ['karma-chai', 'karma-chrome-launcher', 'karma-firefox-launcher', 'karma-mocha', 'karma-mocha-reporter',
      'karma-log-reporter'],


    // list of files / patterns to load in the browser
    files: [
      { pattern : 'src/js/clea.js', type: 'module', included: true} ,
      { pattern : 'test/clea.spec.js', type: 'module', included: true}
    ],


    // list of files / patterns to exclude
    exclude: [
    ],

    // preprocess matching files before serving them to the browser
    // available preprocessors: https://npmjs.org/browse/keyword/karma-preprocessor
    preprocessors: {
    },


    // test results reporter to use
    // possible values: 'dots', 'progress'
    // available reporters: https://npmjs.org/browse/keyword/karma-reporter
    reporters: ['mocha', 'log-reporter'],


    // web server port
    port: 9876,


    // enable / disable colors in the output (reporters and logs)
    colors: true,


    // level of logging
    // possible values: config.LOG_DISABLE || config.LOG_ERROR || config.LOG_WARN || config.LOG_INFO || config.LOG_DEBUG
    logLevel: config.LOG_INFO,


    // enable / disable watching file and executing tests whenever any file changes
    autoWatch: false,


    // start these browsers
    // available browser launchers: https://npmjs.org/browse/keyword/karma-launcher
    browsers: ['FirefoxHeadless','ChromeHeadlessNoSandbox'],
    customLaunchers: {
      FirefoxHeadless: {
        base: 'Firefox',
        flags: [ '-headless' ],
      },
      ChromeHeadlessNoSandbox: {
        base: 'ChromeHeadless',
        flags: ['--no-sandbox']
      }
    },

    // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: true,

    // Concurrency level
    // how many browser should be started simultaneous
    concurrency: Infinity,

  })
}
