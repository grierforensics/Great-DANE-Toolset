app.controller('StartCtrl', function ($scope, $http, $location) {

    $scope.startWorkflow = function() {
        $http.post('/workflow', $scope.email).success(function (data) {
            $scope.workflow = data;
            $location.path("/workflow/"+$scope.workflow.id);
        })
        .error(function (data, status) {
            console.log(data);
            console.log(status);
        });
    };

});