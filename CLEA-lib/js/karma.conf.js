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
    plugins: ['karma-chai', 'karma-browserstack-launcher', 'karma-mocha', 'karma-mocha-reporter',
      'karma-log-reporter'],

    // list of files / patterns to load in the browser
    files: [
      { pattern : 'src/js/clea.js', type: 'module', included: true} ,
      { pattern : 'test/clea.spec.js', type: 'module', included: true},
      { pattern: 'test/dataset.js', included: false},
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
    reporters: ['mocha', 'log-reporter', 'BrowserStack'],

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
    browserStack: {
      username: process.env.BROWSERSTACK_USERNAME,
      accessKey: process.env.BROWSERSTACK_ACCESS_KEY
    },
    customLaunchers: {
      chrome_win_49: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Chrome",
        browser_version : "49.0"
      },
      chrome_win_79: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Chrome",
        browser_version : "79.0"
      },
      chrome_win_80: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Chrome",
        browser_version : "80.0"
      },
      chrome_win_81: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Chrome",
        browser_version : "81.0"
      },
      chrome_win_82: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Chrome",
        browser_version : "82.0"
      },
      chrome_win_83: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Chrome",
        browser_version : "83.0"
      },
      chrome_win_84: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Chrome",
        browser_version : "84.0"
      },
      chrome_win_85: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Chrome",
        browser_version : "85.0"
      },
      chrome_win_86: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Chrome",
        browser_version : "86.0"
      },
      chrome_win_87: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Chrome",
        browser_version : "87.0"
      },
      chrome_win_88: {
        base: 'BrowserStack',
        "os" : "Windows",
        "os_version" : "10",
        "browser" : "Chrome",
        browser_version : "88.0"
      },
      chrome_win_89: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Chrome",
        browser_version : "89.0"
      },
      chrome_win_90: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Chrome",
        browser_version : "90.0"
      },
      safari_mojave_12_1: {
        base: 'BrowserStack',
        os : "OS X",
        os_version : "Mojave",
        browser : "Safari",
        browser_version : "12.1"
      },
      safari_catalina_13_1: {
        base: 'BrowserStack',
        os : "OS X",
        os_version : "Catalina",
        browser : "Safari",
        browser_version : "13.1"
      },
      safari_big_sur_14: {
        base: 'BrowserStack',
        os : "OS X",
        os_version : "Big Sur",
        browser : "Safari",
        browser_version : "14.0"
      },
      firefox_win_71: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "71.0"
      },
      firefox_win_72: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "72.0"
      },
      firefox_win_73: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "73.0"
      },
      firefox_win_74: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "74.0"
      },
      firefox_win_75: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "75.0"
      },
      firefox_win_76: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "76.0"
      },
      firefox_win_77: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "77.0"
      },
      firefox_win_78: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "78.0"
      },
      firefox_win_79: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "79.0"
      },
      firefox_win_80: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "80.0"
      },
      firefox_win_81: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "81.0"
      },
      firefox_win_82: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "82.0"
      },
      firefox_win_83: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "83.0"
      },
      firefox_win_84: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "84.0"
      },
      firefox_win_85: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "85.0"
      },
      firefox_win_86: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "86.0"
      },
      firefox_win_87: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "87.0"
      },
      firefox_win_88: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Firefox",
        browser_version : "88.0"
      },
      edge_win_18: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Edge",
        browser_version : "18.0"
      },
      edge_win_80: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Edge",
        browser_version : "80.0"
      },
      edge_win_81: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Edge",
        browser_version : "81.0"
      },
      edge_win_83: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Edge",
        browser_version : "83.0"
      },
      edge_win_84: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Edge",
        browser_version : "84.0"
      },
      edge_win_85: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Edge",
        browser_version : "85.0"
      },
      edge_win_86: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Edge",
        browser_version : "86.0"
      },
      edge_win_87: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Edge",
        browser_version : "87.0"
      },
      edge_win_88: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Edge",
        browser_version : "88.0"
      },
      edge_win_89: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Edge",
        browser_version : "89.0"
      },
      edge_win_90: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Edge",
        browser_version : "90.0"
      },
      ie_win_11: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "IE",
        browser_version : "11.0"
      },
      opera_win_70: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Opera",
        browser_version : "70.0"
      },
      opera_win_71: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Opera",
        browser_version : "71.0"
      },
      opera_win_72: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Opera",
        browser_version : "72.0"
      },
      opera_win_73: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Opera",
        browser_version : "73.0"
      },
      opera_win_74: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Opera",
        browser_version : "74.0"
      },
      opera_win_75: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Opera",
        browser_version : "75.0"
      },
      opera_win_76: {
        base: 'BrowserStack',
        os : "Windows",
        os_version : "10",
        browser : "Opera",
        browser_version : "76.0"
      }
    },

    browsers: [
        'chrome_win_49',
        'chrome_win_79',
        'chrome_win_80',
        'chrome_win_81',
        'chrome_win_83',
        'chrome_win_84',
        'chrome_win_85',
        'chrome_win_86',
        'chrome_win_87',
        'chrome_win_88',
        'chrome_win_89',
        'chrome_win_90',
        'safari_mojave_12_1',
        'safari_catalina_13_1',
        'safari_big_sur_14',
        'firefox_win_71',
        'firefox_win_72',
        'firefox_win_73',
        'firefox_win_74',
        'firefox_win_75',
        'firefox_win_76',
        'firefox_win_77',
        'firefox_win_78',
        'firefox_win_79',
        'firefox_win_80',
        'firefox_win_81',
        'firefox_win_82',
        'firefox_win_83',
        'firefox_win_84',
        'firefox_win_85',
        'firefox_win_86',
        'firefox_win_87',
        'firefox_win_88',
        'edge_win_18',
        'edge_win_80',
        'edge_win_81',
        'edge_win_83',
        'edge_win_84',
        'edge_win_85',
        'edge_win_86',
        'edge_win_87',
        'edge_win_88',
        'edge_win_89',
        'edge_win_90',
        'ie_win_11',
        'opera_win_70',
        'opera_win_71',
        'opera_win_72',
        'opera_win_73',
        'opera_win_74',
        'opera_win_75',
        'opera_win_76'
    ],

  // Continuous Integration mode
    // if true, Karma captures browsers, runs the tests and exits
    singleRun: true,

    // Concurrency level
    // how many browser should be started simultaneous
    concurrency: Infinity,

  })
}
