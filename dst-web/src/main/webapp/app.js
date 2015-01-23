

var app = angular.module('app', [
    'ngRoute'
]);

app.config(['$routeProvider',
    function($routeProvider) {
        $routeProvider.
            when('/start', {
                templateUrl: 'partials/start.html',
                controller: 'StartCtrl'
            }).
            when('/status/:workflowId', {
                templateUrl: 'partials/status.html',
                controller: 'StatusCtrl'
            }).
            otherwise({
                redirectTo: '/start'
            });
    }]);

