/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.web.action.evaluator;

import org.alfresco.model.ForumModel;
import org.alfresco.web.bean.repository.Node;

/**
 * UI Action Evaluator - Create a forum around a node.
 * 
 * @author Kevin Roast
 */
public class CreateForumNodeEvaluator extends BaseActionEvaluator
{
   private static final long serialVersionUID = -5132048668011887505L;

   /**
    * @see org.alfresco.web.action.ActionEvaluator#evaluate(org.alfresco.web.bean.repository.Node)
    */
   public boolean evaluate(Node node)
   {
      return (node.hasAspect(ForumModel.ASPECT_DISCUSSABLE) == false &&
              node.isLocked() == false);
   }
}
