app.controller('StatusCtrl', function ($scope, $http, $routeParams) {

    $http.get('/workflow/' + $routeParams.workflowId).success(function (data) {
        $scope.workflow = data;
    });
});