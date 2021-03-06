/*******************************************************************************
 * Copyright (C) 2014  Stefan Schroeder
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3.0 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package com.graphhopper.jsprit.core.algorithm.recreate;

import com.graphhopper.jsprit.core.problem.VehicleRoutingProblem;
import com.graphhopper.jsprit.core.problem.job.Job;
import com.graphhopper.jsprit.core.problem.solution.route.VehicleRoute;
import com.graphhopper.jsprit.core.util.NoiseMaker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;


/**
 * Best insertion that insert the job where additional costs are minimal.
 *
 * @author stefan schroeder
 */
public final class BestInsertion extends AbstractInsertionStrategy {

    private static Logger logger = LoggerFactory.getLogger(BestInsertion.class);

    private JobInsertionCostsCalculator bestInsertionCostCalculator;

    private NoiseMaker noiseMaker = new NoiseMaker() {

        @Override
        public double makeNoise() {
            return 0;
        }

    };

    public BestInsertion(JobInsertionCostsCalculator jobInsertionCalculator, VehicleRoutingProblem vehicleRoutingProblem) {
        super(vehicleRoutingProblem);
        bestInsertionCostCalculator = jobInsertionCalculator;
        logger.debug("initialise {}", this);
    }

    @Override
    public String toString() {
        return "[name=bestInsertion]";
    }

    @Override
    public Collection<Job> insertUnassignedJobs(Collection<VehicleRoute> vehicleRoutes, Collection<Job> unassignedJobs) {
        List<Job> badJobs = new ArrayList<Job>(unassignedJobs.size());
        List<Job> unassignedJobList = new ArrayList<Job>(unassignedJobs);
        Collections.shuffle(unassignedJobList, random);
        sometimesSortPriorities(unassignedJobList);
        for (Job unassignedJob : unassignedJobList) {
            Insertion bestInsertion = null;
            double bestInsertionCost = Double.MAX_VALUE;
            for (VehicleRoute vehicleRoute : vehicleRoutes) {
                InsertionData iData = bestInsertionCostCalculator.getInsertionData(vehicleRoute, unassignedJob, NO_NEW_VEHICLE_YET, NO_NEW_DEPARTURE_TIME_YET, NO_NEW_DRIVER_YET, bestInsertionCost);
                if (iData instanceof InsertionData.NoInsertionFound) {
                    continue;
                }
                if (iData.getInsertionCost() < bestInsertionCost + noiseMaker.makeNoise()) {
                    bestInsertion = new Insertion(vehicleRoute, iData);
                    bestInsertionCost = iData.getInsertionCost();
                }
            }
            VehicleRoute newRoute = VehicleRoute.emptyRoute();
            InsertionData newIData = bestInsertionCostCalculator.getInsertionData(newRoute, unassignedJob, NO_NEW_VEHICLE_YET, NO_NEW_DEPARTURE_TIME_YET, NO_NEW_DRIVER_YET, bestInsertionCost);
            if (!(newIData instanceof InsertionData.NoInsertionFound)) {
                if (newIData.getInsertionCost() < bestInsertionCost + noiseMaker.makeNoise()) {
                    bestInsertion = new Insertion(newRoute, newIData);
                    vehicleRoutes.add(newRoute);
                }
            }
            if (bestInsertion == null) badJobs.add(unassignedJob);
            else insertJob(unassignedJob, bestInsertion.getInsertionData(), bestInsertion.getRoute());
//            nextInsertion();
        }
        return badJobs;
    }

    private void sometimesSortPriorities(List<Job> unassignedJobList) {
        if(random.nextDouble() < 0.5){
            Collections.sort(unassignedJobList, new Comparator<Job>() {
                @Override
                public int compare(Job o1, Job o2) {
                    return o1.getPriority() - o2.getPriority();
                }
            });
        }
    }

}
