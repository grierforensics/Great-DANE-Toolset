// Copyright (C) 2016 Grier Forensics. All Rights Reserved.
var app = angular.module('app', [
    'ngRoute'
]);

app.config(['$routeProvider',
    function ($routeProvider) {
        $routeProvider.
            when('/start', {
                templateUrl: 'partials/start.html',
                controller: 'StartCtrl'
            }).
            when('/faq', {
                templateUrl: 'partials/faq.html'
            }).
            when('/api', {
                templateUrl: 'partials/api.html'
            }).
            when('/workflow/:workflowId', {
                templateUrl: 'partials/status.html',
                controller: 'StatusCtrl'
            }).
            when('/dane', {
                templateUrl: 'partials/fetchDaneForm.html'
            }).
            when('/dane/:email', {
                templateUrl: 'partials/fetchDane.html',
                controller: 'FetchDaneCtrl'
            }).
            when('/createDane', {
                templateUrl: 'partials/createDane.html',
                controller: 'CreateDaneCtrl'
            }).
            otherwise({
                redirectTo: '/start'
            });
    }]);

